package ru.nsu.trafficsimulator.model

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class Layout {
    val roads = mutableMapOf<Long, Road>()
    val intersectionRoads = mutableMapOf<Long, IntersectionRoad>()
    val intersections = mutableMapOf<Long, Intersection>()
    val intersectionsList = mutableListOf<Intersection>()

    var roadIdCount: Long = 0
    var intersectionIdCount: Long = 0

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
        val startPoint = startIntersection.position.xzProjection()
        val startDir = startDirection.xzProjection()
        val endPoint = endIntersection.position.xzProjection()
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

    fun moveIntersection(intersection: Intersection, newPosition: Vec3) {
        for (road in intersection.incomingRoads) {
            road.moveRoad(intersection, newPosition)
        }
        intersection.position = newPosition
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
        road.startIntersection?.let {
            it.removeRoad(road)
            if (it.getIncomingRoadsCount() == 0) {
                deleteIntersection(it)
            }
        }
        road.startIntersection?.let {
            it.removeRoad(road)
            if (it.getIncomingRoadsCount() == 0) {
                deleteIntersection(it)
            }
        }
        roads.remove(road.id)
    }

    fun addIntersection(position: Vec3): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection = Intersection(newIntersectionId, position)
        if (!intersections.containsValue(newIntersection)) {
            intersectionsList.add(newIntersection)
        }
        intersections[newIntersectionId] = newIntersection
        return newIntersection
    }

    private fun addIntersectionRoad(intersection: Intersection, fromRoad: Road, toRoad: Road) {
        val incomingLaneNumber = fromRoad.getIncomingLaneNumber(intersection)
        val outgoingLaneNumber = toRoad.getOutgoingLaneNumber(intersection)
        val laneNumber =
            min(abs(incomingLaneNumber), abs(outgoingLaneNumber))

        val geometry = Spline(
            fromRoad.getIntersectionPoint(intersection, laneNumber - abs(incomingLaneNumber)).xzProjection(),
            intersection.position.xzProjection(),
            toRoad.getIntersectionPoint(intersection, abs(outgoingLaneNumber) - laneNumber).xzProjection(),
            intersection.position.xzProjection()
        )

        val newIntersectionRoad = IntersectionRoad(
            id = roadIdCount++,
            intersection = intersection,
            fromRoad = fromRoad,
            toRoad = toRoad,
            lane = laneNumber,
            geometry = geometry
        )
        intersection.intersectionRoads.add(newIntersectionRoad)

        val inSg = incomingLaneNumber.sign
        val outSg = outgoingLaneNumber.sign
        for (lane in laneNumber..1 step -1) {
            newIntersectionRoad.laneLinkage.add(
                Triple(
                    incomingLaneNumber - inSg * lane,
                    lane,
                    outgoingLaneNumber - outSg * lane
                )
            )
        }

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
        intersectionsList.remove(intersection)
    }

    companion object {
        const val DEFAULT_INTERSECTION_PADDING = 10.0
        const val LANE_WIDTH = 4.0
    }
}
