package ru.nsu.trafficsimulator.model

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec2(var x: Double, var y: Double) {
    fun distance(other: Point2): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun rotate(angleRad: Double): Point2 {
        return Point2(x * cos(angleRad) - y * sin(angleRad), x * sin(angleRad) + y * cos(angleRad))
    }

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)
    operator fun times(other: Vec2): Vec2 = Vec2(x * other.x, y * other.y)
    operator fun div(other: Vec2): Vec2 = Vec2(x / other.x, y / other.y)

    operator fun times(scale: Double): Vec2 = Vec2(x * scale, y * scale)
    operator fun div(scale: Double): Vec2 = Vec2(x / scale, y / scale)

    fun dot(other: Vec2): Double = x * other.x + y * other.y
    fun length(): Double = sqrt(lengthSq())
    fun lengthSq(): Double = dot(this)
    fun normalized(): Vec2 = this / length()
}
