package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Vec3
import java.util.*

data class Intersection(
    val id: Long,
    var position: Vec3,
    var padding: Double = 0.0,
    var isBuilding: Boolean = false
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: HashSet<IntersectionRoad> = HashSet()

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

        val irToRemove = LinkedList<IntersectionRoad>()
        for (intersectionRoad in intersectionRoads) {
            if (intersectionRoad.toRoad === road) {
                irToRemove.add(intersectionRoad)
                continue
            }
            if (intersectionRoad.fromRoad === road) {
                irToRemove.add(intersectionRoad)
            }
        }

        for (intersectionRoad in irToRemove) {
            intersectionRoads.remove(intersectionRoad)
        }
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
