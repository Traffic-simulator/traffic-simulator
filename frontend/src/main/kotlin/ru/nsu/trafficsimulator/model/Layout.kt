package ru.nsu.trafficsimulator.model

class Layout {
    val roads = mutableMapOf<Long, Road>()
    val intersectionRoads = mutableMapOf<Long, IntersectionRoad>()
    val intersections = mutableMapOf<Long, Intersection>()

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
        if (!intersections.containsKey(intersection.id)) {
            throw IllegalArgumentException("Intersection with id ${intersection.id} does not exist")
        }

        for (road in intersection.incomingRoads) {
            road.moveRoad(intersection, newPosition)
        }
    }

    private fun connectRoadToIntersection(road: Road, intersection: Intersection) {
        val incomingRoads = intersection.incomingRoads
        for (incomingRoad in incomingRoads) {
            val outIR = IntersectionRoad(
                roadIdCount++,
                intersection,
                road,
                incomingRoad,
                geometry = Spline()
            )
            intersection.intersectionRoads.add(outIR)
            val inIR = IntersectionRoad(
                roadIdCount++,
                intersection,
                incomingRoad,
                road,
                geometry = Spline()
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
