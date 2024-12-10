package ru.nsu.trafficsimulator.model

data class Road(
    val id: Long,
    var startIntersection: Intersection,
    var endIntersection: Intersection,
    var length: Double,
    var leftLane: Int = 1,
    var rightLane: Int = 1,
    var geometry: Spline? = null
) {
    override fun toString(): String {
        return "Road(id=$id, start=${startIntersection.id}, end=${endIntersection.id}, length=$length)"
    }
}
