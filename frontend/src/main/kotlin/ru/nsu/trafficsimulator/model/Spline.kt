package ru.nsu.trafficsimulator.model

import kotlin.math.cos
import kotlin.math.sin

class Spline {
    val x: Poly3
    val y: Poly3
    val length: Double
    val normalized: Boolean


    constructor(
        globalStart: Point2,
        rotationRad: Double,
        length: Double,
        xPoly: Poly3,
        yPoly: Poly3,
        normalized: Boolean
    ) {
        val rotatedX = xPoly * cos(rotationRad) - yPoly * sin(rotationRad)
        val rotatedY = xPoly * sin(rotationRad) + yPoly * cos(rotationRad)
        this.x = Poly3(rotatedX.a + globalStart.x, rotatedX.b, rotatedX.c, rotatedX.d)
        this.y = Poly3(rotatedY.a + globalStart.y, rotatedY.b, rotatedY.c, rotatedY.d)
        this.length = length
        this.normalized = normalized
    }

    constructor(start: Point2, startDirection: Point2, end: Point2, endDirection: Point2) {
        x = Poly3(
            start.x,
            startDirection.x - start.x,
            4 * end.x - 2 * startDirection.x - start.x - endDirection.x,
            endDirection.x + start.x + startDirection.x - 3 * end.x
        )

        y = Poly3(
            start.y,
            startDirection.y - start.y,
            4 * end.y - 2 * startDirection.y - start.y - endDirection.y,
            endDirection.y + start.y + startDirection.y - 3 * end.y
        )

        this.length = calculateLength(x, y)
        this.normalized = true
    }

    fun getPoint(distance: Double): Point2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("distance should be between 0 and length")
        }

        if (normalized) {
            return Point2(x.value(distance / length), x.value(distance / length))
        }
        return Point2(x.value(distance), y.value(distance))
    }


    private fun calculateLength(x: Poly3, y: Poly3): Double {
        val iterations = 1000
        var length = 0.0
        var prev = Point2(x.value(0.0), y.value(0.0))
        val step = 1.0 / iterations
        for (i in 0..iterations) {
            val cur = Point2(x.value(step * i), y.value(step * i))
            length += prev.distance(cur)
            prev = cur
        }
        return length
    }
}
