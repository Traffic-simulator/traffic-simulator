package ru.nsu.trafficsimulator.model

class Layout {
    val layoutRoads = mutableMapOf<Long, Road>()
    val layoutIntersectionRoads = mutableMapOf<Long, IntersectionRoad>()
    val layoutIntersections = mutableMapOf<Long, Intersection>()

    private var roadIdCount: Long = 0
    private var intersectionIdCount: Long = 0

    fun addRoad(road: Road) {
        if (road.id > roadIdCount) {
            roadIdCount = road.id + 1
        }
        layoutRoads[road.id] = road
    }

    fun addIntersectionRoad(intersectionRoad: IntersectionRoad) {
        if (intersectionRoad.id > roadIdCount) {
            roadIdCount = intersectionRoad.id + 1
        }
        layoutIntersectionRoads[intersectionRoad.id] = intersectionRoad
    }

    fun addIntersection(intersection: Intersection) {
        if (intersection.id > intersectionIdCount) {
            intersectionIdCount = intersection.id + 1
        }
        layoutIntersections[intersection.id] = (intersection)
    }

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
        val newRoadId = roadIdCount++
        val length = endIntersection.position.distance(startIntersection.position)
        val newRoad = Road(newRoadId, startIntersection, endIntersection, length)

        connectRoadToIntersection(newRoad, startIntersection)
        connectRoadToIntersection(newRoad, endIntersection)

        layoutRoads[newRoadId] = (newRoad)
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
        val start = road.startIntersection
        start?.let {
            it.removeRoad(road)
            if (it.getIncomingRoadsCount() == 0) {
                deleteIntersection(start)
            }
        }


        val end = road.startIntersection
        end?.let {
            it.removeRoad(road)
            if (it.getIncomingRoadsCount() == 0) {
                deleteIntersection(end)
            }
        }
    }

    private fun addIntersection(position: Point): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection = Intersection(newIntersectionId, position)
        layoutIntersections[newIntersectionId] = (newIntersection)
        return (newIntersection)
    }


    private fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.incomingRoads) {
            deleteRoad(road)
        }
    }
}
