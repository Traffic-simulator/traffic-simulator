package ru.nsu.trafficsimulator.model

import java.util.*
import kotlin.math.*

class Spline {
    val splineParts: MutableList<SplinePart>
    var length: Double
        private set

    init {
        splineParts = LinkedList()
        length = 0.0
    }

    constructor()

    constructor(start: Vec2, startAnchor: Vec2, end: Vec2, endAnchor: Vec2) {
        addSplinePart(start to startAnchor, end to endAnchor)
    }

    fun addPoint(newPoint: Pair<Vec2, Vec2>) {
        val prevPoint = splineParts.last().getEndPoint()
        addSplinePart(prevPoint, newPoint)
    }

    fun addSplinePart(start: Pair<Vec2, Vec2>, end: Pair<Vec2, Vec2>) {
        val (x, y) = normalizedPolynom(start, end)
        val partLength = calculateLength(x, y)
        splineParts.add(SplinePart(x, y, this.length, partLength, true))

        this.length += partLength
    }

    fun addLine(start: Vec2, angle: Double, length: Double) {
        val startVertex = start to start + Vec2(cos(angle), sin(angle))
        val endVertex =
            start + Vec2(cos(angle), sin(angle)) * length to
                start + Vec2(cos(angle), sin(angle)) * (length + 1)
        addSplinePart(startVertex, endVertex)
    }

    fun addParamPoly(
        start: Vec2,
        rotationRad: Double,
        partLength: Double,
        xPoly: Poly3,
        yPoly: Poly3,
        normalized: Boolean
    ) {
        val rotatedX = xPoly * cos(rotationRad) - yPoly * sin(rotationRad)
        val rotatedY = xPoly * sin(rotationRad) + yPoly * cos(rotationRad)
        val x = Poly3(rotatedX.a + start.x, rotatedX.b, rotatedX.c, rotatedX.d)
        val y = Poly3(rotatedY.a + start.y, rotatedY.b, rotatedY.c, rotatedY.d)
        splineParts.add(SplinePart(x, y, this.length, partLength, normalized))
        this.length += partLength
    }

    fun addArc(start: Vec2, startAngle: Double, curvature: Double, length: Double) {
        val maxPart = PI / 2.0
        val deltaAngle = curvature * length
        val parts = ceil(abs(deltaAngle) / maxPart).toInt()
        val step = deltaAngle / parts
        val r = 1 / curvature
        var curPoint = start
        var curAngle = startAngle
        for (i in 0 until parts) {
            val endAngle = curAngle + step

            val endPoint = curPoint - Vec2(
                sin(curAngle) - sin(endAngle),
                -cos(curAngle) + cos(endAngle)
            ) * r

            addSplinePart(
                curPoint to curPoint + Vec2(cos(curAngle), sin(curAngle)) * (length / parts),
                endPoint to endPoint + Vec2(cos(endAngle), sin(endAngle)) * (length / parts)
            )

            curPoint = endPoint
            curAngle = endAngle

        }
    }

    fun getPoint(distance: Double): Vec2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("Offset must be between 0 and length")
        }

        val sp = splineParts.last { it.offset <= distance }
//        println("Current spline part = ${sp}")
        return sp.getPoint(distance - sp.offset)
    }

    fun getDirection(distance: Double): Vec2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("Offset must be between 0 and length")
        }

        val sp = splineParts.last { it.offset <= distance }
        return sp.getDirection(distance - sp.offset)
    }

    fun closestPoint(point: Vec2): Vec2 {
        return splineParts
            .map { it.closestPoint(point) }
            .minBy { closest -> point.distance(closest) }
    }

    fun moveEnd(newPoint: Vec2, newDirection: Vec2) {
        if (splineParts.isEmpty()) throw IllegalArgumentException("SplineParts is empty")

        val lastSP = splineParts.last()
        val (x, y) = normalizedPolynom(lastSP.getStartPoint(), newPoint to newDirection)
        val partLength = calculateLength(x, y)
        splineParts.removeLast()
        splineParts.addLast(SplinePart(x, y, lastSP.offset, partLength, true))
        this.length += partLength - lastSP.length
    }

    fun moveStart(newPoint: Vec2, newDirection: Vec2) {
        if (splineParts.isEmpty()) throw IllegalArgumentException("SplineParts is empty")

        val (x, y) = normalizedPolynom(newPoint to newDirection, splineParts.first().getEndPoint())
        val partLength = calculateLength(x, y)
        splineParts.removeFirst()
        splineParts.addFirst(SplinePart(x, y, 0.0, partLength, true))
        recalculateSplineLength()
    }

    private fun recalculateSplineLength() {
        var length = 0.0
        for (sp in splineParts) {
            sp.offset = length
            length += sp.length
        }
        this.length = length
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

    private fun normalizedPolynom(start: Pair<Vec2, Vec2>, end: Pair<Vec2, Vec2>): Pair<Poly3, Poly3> {
        val (startPoint, startDir) = start
        val (endPoint, endDir) = end

        val startDeriv = startDir - startPoint
        val endDeriv = endDir - endPoint
        if ((startDeriv - endDeriv).lengthSq() < 1e-6) {
            return Poly3(startPoint.x, endPoint.x - startPoint.x, 0.0, 0.0) to Poly3(startPoint.y, endPoint.y - startPoint.y, 0.0, 0.0)
        }

        val x = Poly3(
            startPoint.x,
            startDir.x - startPoint.x,
            4 * endPoint.x - 2 * startDir.x - startPoint.x - endDir.x,
            endDir.x + startPoint.x + startDir.x - 3 * endPoint.x
        )

        val y = Poly3(
            startPoint.y,
            startDir.y - startPoint.y,
            4 * endPoint.y - 2 * startDir.y - startPoint.y - endDir.y,
            endDir.y + startPoint.y + startDir.y - 3 * endPoint.y
        )
        return (x to y)
    }


    class SplinePart(val x: Poly3, val y: Poly3, var offset: Double, val length: Double, val normalized: Boolean) {
        private val endDist = if (normalized) 1.0 else length

        fun getStartPoint(): Pair<Vec2, Vec2> {
            return Vec2(x.value(0.0), y.value(0.0)) to
                Vec2(x.value(0.0), y.value(0.0)) + Vec2(x.derivativeValue(0.0), y.derivativeValue(0.0))
        }

        fun getEndPoint(): Pair<Vec2, Vec2> {
            return Vec2(x.value(endDist), y.value(endDist)) to
                Vec2(x.value(endDist), y.value(endDist)) + Vec2(x.derivativeValue(endDist), y.derivativeValue(endDist))
        }

        fun getPoint(distance: Double): Vec2 {
            if (normalized) {
                return Vec2(x.value(distance / length), y.value(distance / length))
            }
            return Vec2(x.value(distance), y.value(distance))
        }

        fun getDirection(distance: Double): Vec2 {
            if (normalized) {
                return Vec2(x.derivativeValue(distance / length), y.derivativeValue(distance / length))
            }
            return Vec2(x.derivativeValue(distance), y.derivativeValue(distance))
        }

        fun closestPoint(point: Vec2): Vec2 {
            val maxValue = if (normalized) {
                1.0
            } else {
                length
            }
            val iterationCount = 5
            val sampleCount = 10
            val converge = { start: Double ->
                var guess = start
                for (i in 0..<iterationCount) {
                    val xErr = (x.value(guess) - point.x)
                    val yErr = (y.value(guess) - point.y)
                    val xDer = x.derivativeValue(guess)
                    val yDer = y.derivativeValue(guess)
                    guess -= (xErr * xDer + yErr * yDer) / (x.secondDerivativeValue(guess) * xErr + xDer * xDer + y.secondDerivativeValue(
                        guess
                    ) * yErr + yDer * yDer)
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

        override fun toString(): String {
            return "SplinePart(x=$x, y=$y, length=$length, normalized=$normalized offset=$offset)"
        }
    }

    override fun toString(): String {
        var result = "Spline(length=$length, ["
        for (sp in splineParts) {
            result += "\n$sp,"
        }
        result += "\n])"
        return result
    }
}
