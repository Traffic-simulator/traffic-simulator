package ru.nsu.trafficsimulator.model

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Point(val x: Double, val y: Double, val z: Double) {
    fun distance(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

data class Point2(var x: Double, var y: Double) {
    fun distance(other: Point2): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun rotate(angleRad: Double): Point2 {
        return Point2(x * cos(angleRad) - y * sin(angleRad), x * sin(angleRad) + y * cos(angleRad))
    }

    operator fun plus(other: Point2): Point2 {
        return Point2(x + other.x, y + other.y)
    }
}
