package ru.nsu.trafficsimulator.model

data class IntersectionRoad(
    val id: Long,
    val intersection: Intersection,
    val fromRoad: Road,
    val toRoad: Road,
    val lane: Int = 1,
    val geometry: Spline
) {
    val laneLinkage: MutableList<Triple<Int, Int, Int>> = mutableListOf()
}
