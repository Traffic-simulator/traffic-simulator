package ru.nsu.trafficsimulator.model

data class Road(
    val id: Int,
    var startIntersection: Intersection,
    var endIntersection: Intersection,
    var len: Double,
    var leftLane : Int = 1,
    var rightLane : Int = 1,
)
