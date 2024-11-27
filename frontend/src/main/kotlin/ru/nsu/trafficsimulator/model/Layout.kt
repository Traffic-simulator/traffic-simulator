package ru.nsu.trafficsimulator.model

class Layout {
    val roads = mutableSetOf<Road>()
    val intersectionRoads = mutableSetOf<IntersectionRoad>()
    val intersections = mutableSetOf<Intersection>()

    private var roadIdCount: Long = 0
    private var intersectionIdCount: Long = 0

    fun addRoad(startPosition: Point, endPosition: Point): Road {
        val startIntersection = addIntersection(startPosition)
        val endIntersection = addIntersection(endPosition)
        return addRoad(startIntersection, endIntersection)
    }

    fun addRoad(startIntersection: Intersection, endPosition: Point): Road {
        val endIntersection = addIntersection(endPosition)
        return addRoad(startIntersection, endIntersection)
    }

    fun addRoad(startIntersection: Intersection, endIntersection: Intersection): Road {
        val newRoadId = roadIdCount
        val length = endIntersection.position.distance(startIntersection.position)
        val newRoad = Road(newRoadId, startIntersection, endIntersection, length)
        startIntersection.addRoad(newRoad)
        endIntersection.addRoad(newRoad)
        roads.add(newRoad)
        roadIdCount++
        return newRoad
    }

    fun deleteRoad(road: Road) {
        val start = road.startIntersection
        start.removeRoad(road)
        if (start.getIncomingRoadsCount() == 0) {
            deleteIntersection(start)
        }

        val end = road.startIntersection
        end.removeRoad(road)
        if (end.getIncomingRoadsCount() == 0) {
            deleteIntersection(end)
        }

        road.endIntersection.removeRoad(road)
    }

    private fun addIntersection(position: Point): Intersection {
        val newIntersectionId = intersectionIdCount
        val newIntersection = Intersection(newIntersectionId, position)
        intersections.add(newIntersection)
        intersectionIdCount++
        return (newIntersection)
    }


    private fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.getIncomingRoads()) {
            deleteRoad(road)
        }
    }
}
