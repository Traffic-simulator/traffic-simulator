package ru.nsu.trafficsimulator.model

import java.util.*

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

        val irToRemove = LinkedList<IntersectionRoad>()
        for (intersectionRoad in intersectionRoads) {
            if (intersectionRoad.toRoad === road) {
                irToRemove.add(intersectionRoad)
                continue
            }
            if (intersectionRoad.fromRoad === road) {
                incomingRoads.remove(road)
                irToRemove.add(intersectionRoad)
            }
        }

        for (intersectionRoad in irToRemove) {
            intersectionRoads.remove(intersectionRoad)
        }
    }

    fun getIncomingRoadsCount(): Int = incomingRoads.size
}
