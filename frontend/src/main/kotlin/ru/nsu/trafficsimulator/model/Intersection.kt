package ru.nsu.trafficsimulator.model

data class Intersection(
    val id: Long,
    var position: Vec3,
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
    }

    fun getIncomingRoadsCount(): Int = incomingRoads.size
}
