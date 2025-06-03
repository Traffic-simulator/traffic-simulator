package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.editor.logger
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec3

class Layout {
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

        val newRoad = Road(
            id = roadIdCount++,
            startIntersection = startIntersection,
            endIntersection = endIntersection,
            geometry = Spline(startPoint, startDir, endPoint, endDir)
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

        road.startIntersection.connectRoad(road)
        road.endIntersection.connectRoad(road)

        roads[road.id] = road
    }

    fun addBuilding(
        intersection: Intersection, intersectionDirection: Vec3,
        buildingPosition: Vec3, buildingDirection: Vec3,
        building: Building
    ): Road {
        val buildingIntersection = addIntersection(buildingPosition, building)
        return addRoad(intersection, intersectionDirection, buildingIntersection, buildingDirection)
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
                intersections.remove(it.id)
            }
        }
        road.endIntersection.let {
            it.removeRoad(road)
            if (it.incomingRoadsCount == 0) {
                intersections.remove(it.id)
            }
        }
        roads.remove(road.id)
    }

    fun addIntersection(position: Vec3, building: Building? = null): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection =
            Intersection(newIntersectionId, position.xzProjection(), DEFAULT_INTERSECTION_PADDING, building)
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
        for (road in intersection.incomingRoads) {
            deleteRoad(road)
        }
        intersections.remove(intersection.id)
    }

    /**
     * Only for adding a road without any additional actions.
     */
    fun pushRoad(road: Road) {
        if (roads.containsKey(road.id))
            throw IllegalArgumentException("Road id already exists, can't push road")

        roads[road.id] = road
    }

    /**
     * Only for adding a road without any additional actions.
     */
    fun pushIntersection(intersection: Intersection) {
        if (intersections.containsKey(intersection.id))
            throw IllegalArgumentException("Intersection id already exists, can't push intersection")

        intersections[intersection.id] = intersection
    }

    companion object {
        const val DEFAULT_INTERSECTION_PADDING = 20.0
        const val LANE_WIDTH = 4.0
    }
}
