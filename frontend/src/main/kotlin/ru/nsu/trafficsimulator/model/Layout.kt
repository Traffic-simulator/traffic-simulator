package ru.nsu.trafficsimulator.model

class Layout {
    private val roads = mutableSetOf<Road>()
    private val intersections = mutableSetOf<Intersection>()

    private var roadIdCount = 0
    private var intersectionIdCount = 0

    fun getRoads(): Set<Road> = roads.toSet()
    fun getIntersections(): Set<Intersection> = intersections.toSet()

    fun addRoad(startPosition: Vec3, endPosition: Vec3): Road {
        val startIntersection = addIntersection(startPosition)
        val endIntersection = addIntersection(endPosition)
        return addRoad(startIntersection, endIntersection)
    }

    fun addRoad(startIntersection: Intersection, endPosition: Vec3): Road {
        val endIntersection = addIntersection(endPosition)
        return addRoad(startIntersection, endIntersection)
    }
    fun addRoad(startPosition: Vec3, endIntersection: Intersection): Road {
        return addRoad(endIntersection, startPosition)
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

    fun addIntersection(position: Vec3): Intersection {
        val newIntersectionId = intersectionIdCount
        val newIntersection = Intersection(newIntersectionId, position)
        intersections.add(newIntersection)
        intersectionIdCount++
        return (newIntersection)
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

    fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.getIncomingRoads()) {
            deleteRoad(road)
        }
    }
}
