package ru.nsu.trafficsimulator.model

import kotlin.math.cos
import kotlin.math.sin

class Spline {
    val x: Poly3
    val y: Poly3
    val length: Double
    val normalized: Boolean


    constructor(
        globalStart: Vec2,
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

    constructor(start: Vec2, startAnchor: Vec2, end: Vec2, endAnchor: Vec2) {
        val startNext = start + (start - startAnchor)
        x = Poly3(
            start.x,
            startNext.x - start.x,
            4 * end.x - 2 * startNext.x - start.x - endAnchor.x,
            endAnchor.x + start.x + startNext.x - 3 * end.x
        )

        y = Poly3(
            start.y,
            startNext.y - start.y,
            4 * end.y - 2 * startNext.y - start.y - endAnchor.y,
            endAnchor.y + start.y + startNext.y - 3 * end.y
        )

        this.length = calculateLength(x, y)
        this.normalized = true
    }

    fun getPoint(distance: Double): Vec2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("distance should be between 0 and length")
        }

        if (normalized) {
            return Vec2(x.value(distance / length), y.value(distance / length))
        }
        return Vec2(x.value(distance), y.value(distance))
    }

    fun getDirection(distance: Double): Vec2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("distance should be between 0 and length")
        }

        if (normalized) {
            return Vec2(x.derivativeValue(distance / length), y.derivativeValue(distance / length))
        }
        return Vec2(x.derivativeValue(distance), y.derivativeValue(distance))
    }

    fun closestPoint(point: Vec2): Vec2 {
        val maxValue = if (normalized) { 1.0 } else { length }
        val iterationCount = 5
        val sampleCount = 10
        val converge = { start: Double ->
            var guess = start
            for (i in 0..<iterationCount) {
                val xErr = (x.value(guess) - point.x)
                val yErr = (y.value(guess) - point.y)
                val xDer = x.derivativeValue(guess)
                val yDer = y.derivativeValue(guess)
                guess -= (xErr * xDer + yErr * yDer) / (x.secondDerivativeValue(guess) * xErr + xDer * xDer + y.secondDerivativeValue(guess) * yErr + yDer * yDer)
            }
            guess
        }
        val distanceSqFrom = { t: Double ->
            val dx = x.value(t) - point.x
            val dy = y.value(t) - point.y
            dx * dx + dy * dy
        }
        var bestGuess = 0.0
        var bestDistSq = distanceSqFrom(0.0)
        val distFromMax = distanceSqFrom(maxValue)
        if (distFromMax < bestDistSq) {
            bestGuess = maxValue
            bestDistSq = distFromMax
        }
        for (i in 0..<sampleCount) {
            val guess = converge(maxValue * i / sampleCount.toDouble())
            val dist = distanceSqFrom(guess)
            if (guess > 0.0 && guess < 1.0 && dist < bestDistSq) {
                bestGuess = guess
                bestDistSq = dist
            }
        }

        return Vec2(x.value(bestGuess), y.value(bestGuess))
    }

    private fun calculateLength(x: Poly3, y: Poly3): Double {
        val iterations = 1000
        var length = 0.0
        var prev = Vec2(x.value(0.0), y.value(0.0))
        val step = 1.0 / iterations
        for (i in 0..iterations) {
            val cur = Vec2(x.value(step * i), y.value(step * i))
            length += prev.distance(cur)
            prev = cur
        }
        return length
    }

    override fun toString(): String {
        return "Spline(x=$x, y=$y, length=$length, normalized=$normalized)"
    }
}
