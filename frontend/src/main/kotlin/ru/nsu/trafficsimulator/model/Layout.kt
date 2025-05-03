package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec3
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class Layout {
    val roads = mutableMapOf<Long, Road>()
    val intersectionRoads = mutableMapOf<Long, IntersectionRoad>()
    val intersections = mutableMapOf<Long, Intersection>()

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
        intersectionRoads.clear()
        for ((key, value) in other.intersectionRoads) {
            intersectionRoads[key] = value
        }

        roadIdCount = other.roadIdCount
        intersectionIdCount = other.intersectionIdCount
    }

    fun addRoad(startPosition: Vec3, startDirection: Vec3, endPosition: Vec3, endDirection: Vec3): Road {
        val startIntersection = addIntersection(startPosition)
        val endIntersection = addIntersection(endPosition)
        return addRoad(startIntersection, startDirection, endIntersection, endDirection)
    }

    fun addRoad(startIntersection: Intersection, startDirection: Vec3, endPosition: Vec3, endDirection: Vec3): Road {
        val endIntersection = addIntersection(endPosition)
        return addRoad(startIntersection, startDirection, endIntersection, endDirection)
    }

    fun addRoad(startPosition: Vec3, startDirection: Vec3, endIntersection: Intersection, endDirection: Vec3): Road {
        val startIntersection = addIntersection(startPosition)
        return addRoad(startIntersection, startDirection, endIntersection, endDirection)
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

        connectRoadToIntersection(newRoad, startIntersection)
        connectRoadToIntersection(newRoad, endIntersection)

        roads[newRoad.id] = newRoad
        return newRoad
    }

    fun addBuilding(
        intersection: Intersection, intersectionDirection: Vec3,
        buildingPosition: Vec3, buildingDirection: Vec3,
        building : Building): Road {
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

    private fun connectRoadToIntersection(road: Road, intersection: Intersection) {
        val incomingRoads = intersection.incomingRoads
        for (incomingRoad in incomingRoads) {
            addIntersectionRoad(intersection, road, incomingRoad)
            addIntersectionRoad(intersection, incomingRoad, road)
        }
        intersection.addRoad(road)
    }

    fun deleteRoad(road: Road) {
        road.startIntersection.let {
            it.removeRoad(road)
            if (it.getIncomingRoadsCount() == 0) {
                deleteIntersection(it)
            }
        }
        road.endIntersection.let {
            it.removeRoad(road)
            if (it.getIncomingRoadsCount() == 0) {
                deleteIntersection(it)
            }
        }
        roads.remove(road.id)
    }

    fun addIntersection(position: Vec3, building: Building? = null): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection = Intersection(newIntersectionId, position.xzProjection(), DEFAULT_INTERSECTION_PADDING, building)
        intersections[newIntersectionId] = newIntersection
        return newIntersection
    }

    private fun addIntersectionRoad(intersection: Intersection, fromRoad: Road, toRoad: Road) {
        val incomingLaneNumber = fromRoad.getIncomingLaneNumber(intersection)
        val outgoingLaneNumber = toRoad.getOutgoingLaneNumber(intersection)

        val incomingSign = if (incomingLaneNumber > 0) 1 else -1
        val outgoingSign = if (outgoingLaneNumber > 0) 1 else -1

        val dirLength1 = fromRoad.getIntersectionPoint(intersection).distance(intersection.position)
        val dirLength2 = toRoad.getIntersectionPoint(intersection).distance(intersection.position)

        for (incomingLane in 0..<abs(incomingLaneNumber)) {
            for (outgoingLane in 0..<abs(outgoingLaneNumber)) {
                val il = incomingLane * incomingSign
                val ol = outgoingLane * outgoingSign

                val geometry = Spline(
                    fromRoad.getIntersectionPoint(intersection, -abs(il)).xzProjection(),
                    fromRoad.getIntersectionPoint(intersection, -abs(il)).xzProjection()
                        + fromRoad.getIntersectionDirection(intersection, true).xzProjection()
                        .setLength(dirLength1),
                    toRoad.getIntersectionPoint(intersection, abs(ol)).xzProjection(),
                    toRoad.getIntersectionPoint(intersection, abs(ol)).xzProjection()
                        + toRoad.getIntersectionDirection(intersection, false).xzProjection()
                        .setLength(dirLength2)
                )

                val newIntersectionRoad = IntersectionRoad(
                    id = roadIdCount++,
                    intersection = intersection,
                    fromRoad = fromRoad,
                    toRoad = toRoad,
                    geometry = geometry,
                    laneLinkage = (incomingLane + 1) * incomingSign to (outgoingLane + 1) * outgoingSign
                )

                intersection.intersectionRoads.add(newIntersectionRoad)
                intersectionRoads[newIntersectionRoad.id] = newIntersectionRoad
            }
        }
    }

    fun roadSetLaneNumber(road: Road, leftLane: Int = road.leftLane, rightLane: Int = road.rightLane) {
        road.leftLane = leftLane
        road.rightLane = rightLane

        road.startIntersection?.let {
            connectRoadToIntersection(road, it)
        }
        road.endIntersection?.let {
            connectRoadToIntersection(road, it)
        }
    }

    override fun toString(): String {
        val roadsString = roads.values.joinToString(", ") { it.toString() }
        val intersectionsString = intersections.values.joinToString(", ") { it.toString() }
        return "Layout(roads=[$roadsString], intersections=[$intersectionsString], " +
            "roadIdCount=$roadIdCount, intersectionIdCount=$intersectionIdCount), intersectionRoads=$intersectionRoads"

    }

    private fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.incomingRoads) {
            deleteRoad(road)
        }
        intersections.remove(intersection.id)
    }

    companion object {
        const val DEFAULT_INTERSECTION_PADDING = 10.0
        const val LANE_WIDTH = 4.0
    }
}
