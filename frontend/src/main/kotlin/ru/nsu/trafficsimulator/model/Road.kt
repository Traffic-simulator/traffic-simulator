package ru.nsu.trafficsimulator.model

data class Road(
    val id: Long,
    val startIntersection: Intersection?,
    val endIntersection: Intersection?,
    val length: Double,
    val leftLane: Int = 1,
    val rightLane: Int = 1,
    val geometry: Spline? = null
)
