package ru.nsu.trafficsimulator.model

class Layout {
    val roads = mutableSetOf<Road>()
    val intersectionRoads = mutableSetOf<IntersectionRoad>()
    val intersections = mutableSetOf<Intersection>()
    val intersectionsList = mutableListOf<Intersection>()

    private var roadIdCount: Long = 0
    private var intersectionIdCount: Long = 0

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

        roads.add(newRoad)
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

    fun addIntersection(position: Vec3): Intersection {
        val newIntersectionId = intersectionIdCount++
        val newIntersection = Intersection(newIntersectionId, position)
        if (!intersections.contains(newIntersection)) {
            intersectionsList.add(newIntersection)
        }
        intersections.add(newIntersection)
        return newIntersection
    }

    override fun toString(): String {
        val roadsInfo = roads.joinToString(separator = "\n") { road ->
            "Road(id=${road.id}, startIntersection=${road.startIntersection.id}, endIntersection=${road.endIntersection.id}, length=${road.length})"
        }

        val intersectionsInfo = intersections.joinToString(separator = "\n") { intersection ->
            "Intersection(id=${intersection.id}, position=${intersection.position})"
        }

        return buildString {
            append("Layout:\n")
            append("Intersections:\n")
            append(intersectionsInfo)
            append("\nRoads:\n")
            append(roadsInfo)
        }
    }


    private fun deleteIntersection(intersection: Intersection) {
        for (road in intersection.incomingRoads) {
            deleteRoad(road)
        }
    }
}
