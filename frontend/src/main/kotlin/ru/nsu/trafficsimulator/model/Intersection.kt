package ru.nsu.trafficsimulator.model

data class Intersection(
    val id: Long,
    var position: Point,
    var buildingId: Int? = null
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: HashSet<IntersectionRoad> = HashSet()


    fun addRoad(road: Road) {
        incomingRoads.add(road)
    }

    fun removeRoad(road: Road) {
        incomingRoads.remove(road)
    }

    fun getIncomingRoads(): Set<Road> = incomingRoads.toSet()

    fun getIncomingRoadsCount(): Int = incomingRoads.size
}
