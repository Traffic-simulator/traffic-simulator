package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.intsettings.BuildingIntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.IntersectionSettings
import kotlin.math.max

class Layout(district: Int = 0) {
    var district = district
        private set
    val roads = mutableMapOf<Long, Road>()
    val intersections = mutableMapOf<Long, Intersection>()
    val intersectionRoadsNumber
        get() = intersections.values.sumOf { it.intersectionRoads.size }

    var roadIdCount: Long = 0
    var intersectionIdCount: Long = 0

    fun copy(other: Layout) {
        roads.clear()
        for ((key, value) in other.roads) {
            roads[key] = value
        }
        intersections.clear()
        for ((key, value) in other.intersections) {
            intersections[key] = value
        }

        roadIdCount = other.roadIdCount
        intersectionIdCount = other.intersectionIdCount

        district = other.district
    }

    fun addRoad(
        startIntersection: Intersection,
        startDirection: Vec3,
        endIntersection: Intersection,
        endDirection: Vec3
    ): Road {
        val startPoint = startIntersection.position
        val startDir = startDirection.xzProjection()
        val endPoint = endIntersection.position
        val endDir = endDirection.xzProjection()

        val spline = Spline(startPoint, startDir, endPoint, endDir)

        if (spline.length < startIntersection.padding + endIntersection.padding) {
            throw IllegalArgumentException(
                "Spline length is ${spline.length}, less than " +
                    "${startIntersection.padding} + ${endIntersection.padding}"
            )
        }

        val newRoad = Road(
            id = roadIdCount++,
            startIntersection = startIntersection,
            endIntersection = endIntersection,
            geometry = spline,
            district = district
        )

        addRoad(newRoad)

        return newRoad
    }

    fun addRoad(road: Road) {
        if (roads.containsKey(road.id)) {
            logger.warn("Tried to add a road that is already in the layout")
        }

        if (road.startIntersection.hasSignals) {
            road.startIntersection.signals[road] = Signal()
        }
        if (road.endIntersection.hasSignals) {
            road.endIntersection.signals[road] = Signal()
        }

        if (!intersections.containsKey(road.startIntersection.id)) {
            throw Exception("Adding road with intersection outside the layout")
        }
        if (!intersections.containsKey(road.endIntersection.id)) {
            throw Exception("Adding road with intersection outside the layout")
        }
        road.startIntersection.connectRoad(road)
        road.endIntersection.connectRoad(road)

        roads[road.id] = road
    }

    fun addBuilding(
        intersection: Intersection, intersectionDirection: Vec3,
        buildingPosition: Vec3, buildingDirection: Vec3,
        building: BuildingIntersectionSettings
    ): Pair<Intersection, Road> {
        val buildingIntersection = addIntersection(buildingPosition, building)
        try {
            val road = addRoad(intersection, intersectionDirection, buildingIntersection, buildingDirection)
            return buildingIntersection to road
        } catch (e: Exception) {
            deleteIntersection(buildingIntersection)
            throw e
        }
    }

    fun moveIntersection(intersection: Intersection, newPosition: Vec3) {
        for (road in intersection.incomingRoads) {
            road.moveRoad(intersection, newPosition)
            if (road.startIntersection != intersection)
                road.startIntersection.recalculateIntersectionRoads()
            if (road.endIntersection != intersection)
                road.endIntersection.recalculateIntersectionRoads()
        }
        intersection.position = newPosition.xzProjection()
        intersection.recalculateIntersectionRoads()
    }

    fun deleteRoad(road: Road) {
        road.startIntersection.let {
            it.removeRoad(road)
            if (it.incomingRoadsCount == 0) {
                deleteIntersection(it)
            }
        }
        road.endIntersection.let {
            it.removeRoad(road)
            if (it.incomingRoadsCount == 0) {
                deleteIntersection(it)
            }
        }
        roads.remove(road.id)
    }

    fun addIntersection(position: Vec3, intersectionSettings: IntersectionSettings): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection =
            Intersection(newIntersectionId, position.xzProjection(), DEFAULT_INTERSECTION_PADDING, intersectionSettings)
        intersections[newIntersectionId] = newIntersection
        return newIntersection
    }

    fun roadSetLaneNumber(road: Road, leftLane: Int = road.leftLane, rightLane: Int = road.rightLane) {
        road.leftLane = leftLane
        road.rightLane = rightLane

        road.startIntersection.connectRoad(road)
        road.endIntersection.connectRoad(road)
    }

    override fun toString(): String {
        val roadsString = roads.values.joinToString(", ") { it.toString() }
        val intersectionsString = intersections.values.joinToString(", ") { it.toString() }
        return "Layout(roads=[$roadsString], intersections=[$intersectionsString], " +
            "roadIdCount=$roadIdCount, intersectionIdCount=$intersectionIdCount)"

    }

    private fun deleteIntersection(intersection: Intersection) {
        if (intersection.isMerging) {
            return
        }

        for (road in intersection.incomingRoads) {
            deleteRoad(road)
        }
        intersections.remove(intersection.id)
    }

    /**
     * Only for adding a road without any additional actions.
     */
    fun pushRoad(road: Road, onConflictChangeId: Boolean = false) {
        if (roads.containsKey(road.id)) {
            if (onConflictChangeId) {
                road.id = roadIdCount++
                roads[road.id] = road
            } else {
                throw IllegalArgumentException("Road id already exists, can't push road")
            }
        }

        roadIdCount = max(roadIdCount, road.id + 1)
        roads[road.id] = road
    }

    /**
     * Only for adding a road without any additional actions.
     */
    fun pushIntersection(intersection: Intersection, onConflictChangeId: Boolean = false) {
        if (intersections.containsKey(intersection.id))
            if (onConflictChangeId) {
                intersection.id = intersectionIdCount++
                intersections[intersection.id] = intersection
            } else {
                throw IllegalArgumentException("Intersection id already exists, can't push intersection")
            }

        intersectionIdCount = max(intersectionIdCount, intersection.id + 1)
        intersections[intersection.id] = intersection
    }

    companion object {
        const val DEFAULT_INTERSECTION_PADDING = 10.0
        const val LANE_WIDTH = 4.0
    }
}
