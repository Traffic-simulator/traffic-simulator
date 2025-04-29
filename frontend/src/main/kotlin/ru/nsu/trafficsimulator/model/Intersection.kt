package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Vec3

data class Intersection(
    val id: Long,
    var position: Vec3,
    var padding: Double = 0.0,
    var buildingId: Int? = null
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: HashSet<IntersectionRoad> = HashSet()

    override fun toString(): String {
        return "Intersection(id=$id, position=$position)"
    }

    fun addRoad(road: Road) {
        incomingRoads.add(road)
    }

    fun removeRoad(road: Road) {
        incomingRoads.remove(road)
        intersectionRoads.removeIf { it.toRoad === road || it.fromRoad === road }
    }

    fun recalculateIntersectionRoads() {
        for (intersectionRoad in intersectionRoads) {
            intersectionRoad.recalculateGeometry()
        }
    }

    fun recalculateIntersectionRoads(road : Road) {
        for (intersectionRoad in intersectionRoads) {
            if (intersectionRoad.fromRoad === road || intersectionRoad.toRoad === road) {
                intersectionRoad.recalculateGeometry()
            }
        }
    }

    fun getIncomingRoadsCount(): Int = incomingRoads.size
}
