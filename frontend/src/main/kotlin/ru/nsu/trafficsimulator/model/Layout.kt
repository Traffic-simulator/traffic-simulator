package ru.nsu.trafficsimulator.model

class Layout {
    val roads = mutableMapOf<Long, Road>()
    val intersectionRoads = mutableMapOf<Long, IntersectionRoad>()
    val intersections = mutableMapOf<Long, Intersection>()

    private var roadIdCount: Long = 0
    private var intersectionIdCount: Long = 0

    fun pushRoad(road: Road) {
        if (roads.containsKey(road.id) || intersectionRoads.containsKey(road.id)) {
            throw IllegalArgumentException("Road with id ${road.id} already exists.")
        }
        if (road.id > roadIdCount) {
            roadIdCount = road.id + 1
        }
        roads[road.id] = road
        road.endIntersection?.incomingRoads?.add(road)
        road.startIntersection?.incomingRoads?.add(road)
    }

    fun pushIntersection(intersection: Intersection) {
        if (roads.containsKey(intersection.id)) {
            throw IllegalArgumentException("Intersection with id ${intersection.id} already exists.")
        }
        if (intersection.id > roadIdCount) {
            roadIdCount = intersection.id + 1
        }
        intersections[intersection.id] = intersection
    }

    fun pushIntersectionRoad(road: IntersectionRoad) {
        if (roads.containsKey(road.id) || intersectionRoads.containsKey(road.id)) {
            throw IllegalArgumentException("Road with id ${road.id} already exists.")
        }
        if (road.id > roadIdCount) {
            roadIdCount = road.id + 1
        }
        intersectionRoads[road.id] = road
        road.intersection.intersectionRoads.add(road)
    }

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
        val newRoadId = roadIdCount++
        val length = endIntersection.position.distance(startIntersection.position)
        val newRoad = Road(newRoadId, startIntersection, endIntersection, length)

        connectRoadToIntersection(newRoad, startIntersection)
        connectRoadToIntersection(newRoad, endIntersection)

        roads[newRoadId] = newRoad
        return newRoad
    }

    private fun connectRoadToIntersection(road: Road, intersection: Intersection) {
        val incomingRoads = intersection.incomingRoads
        for (incomingRoad in incomingRoads) {
            val outIR = IntersectionRoad(
                roadIdCount++,
                intersection,
                road,
                incomingRoad,
                1.0
            )
            intersection.intersectionRoads.add(outIR)
            val inIR = IntersectionRoad(
                roadIdCount++,
                intersection,
                incomingRoad,
                road,
                1.0
            )
            intersection.intersectionRoads.add(inIR)
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
    }

    fun addIntersection(position: Vec3): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection = Intersection(newIntersectionId, position)
        intersections[newIntersectionId] = newIntersection
        return newIntersection
    }


    private fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.incomingRoads) {
            deleteRoad(road)
        }
    }
}
