package ru.nsu.trafficsimulator.math

import com.badlogic.gdx.math.Vector3
import kotlin.math.sqrt
import kotlin.math.abs

data class Vec3(var x: Double, var y: Double, var z: Double) {
    constructor() : this(0.0, 0.0, 0.0) {}
    constructor(vec: Vector3) : this(vec.x, vec.y, vec.z) {}
    constructor(x: Float, y: Float, z: Float) : this(x.toDouble(), y.toDouble(), z.toDouble()) {}
    fun distance(other: Vec3): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)
    operator fun times(other: Vec3): Vec3 = Vec3(x * other.x, y * other.y, z * other.z)
    operator fun div(other: Vec3): Vec3 = Vec3(x / other.x, y / other.y, z / other.z)

    operator fun times(scale: Double): Vec3 = Vec3(x * scale, y * scale, z * scale)
    operator fun div(scale: Double): Vec3 = Vec3(x / scale, y / scale, z / scale)
    fun abs(): Vec3 = Vec3(abs(x), abs(y), abs(z))
    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
    fun cross(other: Vec3): Vec3 = Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)
    fun length(): Double = sqrt(x * x + y * y + z * z)
    fun lengthSq(): Double = dot(this)
    fun normalized(): Vec3 = this / length()
    fun toGdxVec(): Vector3 = Vector3(x.toFloat(), y.toFloat(), z.toFloat())
    fun xzProjection() = Vec2(x, -z)
    fun setLength(length: Double) = this * (length / length())

    companion object {
        val UP: Vec3 = Vec3(0.0, 1.0, 0.0)
    }
}
