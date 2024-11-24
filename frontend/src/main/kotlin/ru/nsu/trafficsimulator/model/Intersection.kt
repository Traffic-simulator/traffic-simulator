package ru.nsu.trafficsimulator.model

data class Intersection(
    val id: Int,
    var position: Vec3,
    var buildingId: Int? = null
) {
    private val incomingRoads: MutableSet<Road> = HashSet()

    fun addRoad(road: Road) {
        incomingRoads.add(road)
    }

    fun removeRoad(road: Road) {
        incomingRoads.remove(road)
    }

    fun getIncomingRoads(): Set<Road> = incomingRoads.toSet()

    fun getIncomingRoadsCount(): Int = incomingRoads.size
}
