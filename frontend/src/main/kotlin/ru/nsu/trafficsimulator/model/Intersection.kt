package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Vec3

data class Intersection(
    val id: Long,
    var position: Vec3,
    var padding: Double = 0.0,
    var building: Building? = null
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: HashSet<IntersectionRoad> = HashSet()
    val isBuilding: Boolean get() = building != null

    override fun toString(): String {
        return "Intersection(id=$id, position=$position)"
    }

    fun addRoad(road: Road) {
        if (isBuilding && incomingRoads.size > 0) {
            throw IllegalArgumentException("Building cannot have more than one road")
        }
        incomingRoads.add(road)
    }

    fun removeRoad(road: Road) {
        if (isBuilding) {
            throw IllegalArgumentException("Cannot remove road from building")
        }

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
