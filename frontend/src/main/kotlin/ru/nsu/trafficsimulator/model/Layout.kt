package ru.nsu.trafficsimulator.model

class Layout {
    val layoutRoads = mutableSetOf<Road>()
    val layoutIntersectionRoads = mutableSetOf<IntersectionRoad>()
    val layoutIntersections = mutableSetOf<Intersection>()

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
        val newRoadId = roadIdCount++
        val length = endIntersection.position.distance(startIntersection.position)
        val newRoad = Road(newRoadId, startIntersection, endIntersection, length)

        connectRoadToIntersection(newRoad, startIntersection)
        connectRoadToIntersection(newRoad, endIntersection)

        layoutRoads.add(newRoad)
        return newRoad
    }

    private fun connectRoadToIntersection(road: Road, intersection: Intersection) {
        val incomingRoads = intersection.getIncomingRoads()
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
        val newIntersectionId = intersectionIdCount++
        val newIntersection = Intersection(newIntersectionId, position)
        layoutIntersections.add(newIntersection)
        return (newIntersection)
    }


    private fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.getIncomingRoads()) {
            deleteRoad(road)
        }
    }
}
