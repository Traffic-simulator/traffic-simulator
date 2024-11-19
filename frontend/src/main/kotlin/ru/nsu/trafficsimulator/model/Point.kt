package ru.nsu.trafficsimulator.model

import kotlin.math.sqrt

data class Point(val x: Double, val y: Double, val z: Double) {
    fun distance(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
