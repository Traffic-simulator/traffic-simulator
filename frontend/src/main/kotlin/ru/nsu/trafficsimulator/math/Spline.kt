package ru.nsu.trafficsimulator.math

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

    fun addSpiral(start: Vec2, startAngle: Double, startCurvature: Double, endCurvature: Double, length: Double) {
        val maxPart = PI / 2.0
        val integralPartCount = 100
        val curvatureK = (endCurvature - startCurvature) / length
        val getPartLength = { startPartAngle: Double, endPartAngle: Double, startPartCurv: Double ->
            val D = (startPartCurv / curvatureK).pow(2.0) + (endPartAngle - startPartAngle) / curvatureK
            val root1 = (-startPartCurv / curvatureK - sqrt(D))
            val root2 = (-startPartCurv / curvatureK + sqrt(D))
            if (root1 > 0.0 && (root2 < 0.0 || root1 < root2)) {
                root1
            } else if (root2 > 0.0 && (root1 < 0.0 || root2 < root1)) {
                root2
            } else {
                0.0
            }
        }
        val deltaAngle = ((startCurvature + endCurvature) / 2) * length
        val parts = ceil(abs(deltaAngle) / maxPart).toInt()
        val step = deltaAngle / parts
        var curPoint = start
        var curAngle = startAngle
        var curCurvature = startCurvature
        for (i in 0 until parts) {
            val endAngle = curAngle + step

            var leftCurvature = curCurvature
            var leftAngle = curAngle
            var endPoint = curPoint
            for (j in 0 until integralPartCount) {
                val rightAngle = leftAngle + step / integralPartCount
                val partLength = getPartLength(leftAngle, rightAngle, leftCurvature)
                if (partLength < 1e-6 && j != integralPartCount - 1) {
                    throw Exception("Part #$j is too small? $partLength $leftAngle $rightAngle $leftCurvature $curvatureK")
                }
                val rightCurvature = leftCurvature + curvatureK * partLength
                if (abs((rightCurvature + leftCurvature) / 2 * partLength + leftAngle - rightAngle) < 1e-6) {
                    throw Exception("Double-check did not pass")
                }
                val avgCurvature = (leftCurvature + rightCurvature) / 2

                if (abs(avgCurvature) < 1e-6) {
                    throw Exception("Curvature is too small? $avgCurvature")
                }

                val r = 1 / avgCurvature

                endPoint -= Vec2(
                    sin(leftAngle) - sin(rightAngle),
                    -cos(leftAngle) + cos(rightAngle)
                ) * r

                leftAngle = rightAngle
                leftCurvature = rightCurvature
            }

            val partLength = getPartLength(curAngle, endAngle, curCurvature)
            addSplinePart(
                curPoint to curPoint + Vec2(cos(curAngle), sin(curAngle)) * partLength,
                endPoint to endPoint + Vec2(cos(endAngle), sin(endAngle)) * partLength
            )

            curPoint = endPoint
            curAngle = endAngle
            curCurvature += curvatureK * partLength
        }
    }

    fun getPoint(distance: Double): Vec2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("Offset must be between 0 and length")
        }

        val sp = splineParts.last { it.offset <= distance }
        return sp.getPoint(distance - sp.offset)
    }

    fun getDirection(distance: Double): Vec2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("Offset must be between 0 and length")
        }

        val sp = splineParts.last { it.offset <= distance }
        return sp.getDirection(distance - sp.offset)
    }

    /**
     * @return Pair of closest point and direction at that point
     */
    fun closestPoint(point: Vec2): Pair<Vec2, Vec2> {
        return splineParts
            .map { it.closestPoint(point) }
            .minBy { (closestPoint, _) -> point.distance(closestPoint) }
    }

    /**
     * @return Triple of closest point, direction at that point and distance from the start of the spline part
     */
    fun closestPointWithDistance(point: Vec2): Triple<Vec2, Vec2, Double> {
        return splineParts
            .map { it.closestPointWithDistance(point) }
            .mapIndexed { i, res -> Triple(res.first, res.second, res.third + splineParts[i].offset) }
            .minBy { (closestPoint, _) -> point.distance(closestPoint) }
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

        splineParts[0] = SplinePart(x, y, 0.0, partLength, true)
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

    private fun normalizedPolynom(start: Pair<Vec2, Vec2>, end: Pair<Vec2, Vec2>): Pair<Poly3, Poly3> {
        val (startPoint, startDir) = start
        val (endPoint, endDir) = end

        val startDeriv = startDir - startPoint
        val endDeriv = endDir - endPoint
        if ((startDeriv - endDeriv).lengthSq() < 1e-6) {
            return Poly3(startPoint.x, endPoint.x - startPoint.x, 0.0, 0.0) to Poly3(
                startPoint.y,
                endPoint.y - startPoint.y,
                0.0,
                0.0
            )
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

    fun copy(startPadding: Double = 0.0, endPadding: Double = 0.0): Spline {
        if (startPadding + endPadding >= length) {
            throw IllegalArgumentException("Padding sum must be between 0 and length")
        }

        val startIdx = splineParts.indexOfFirst { it.offset + it.length > startPadding }
        val endIdx = splineParts.indexOfLast { it.offset < length - endPadding }

        if (startIdx == endIdx) {
            val sp = splineParts[startIdx]

            return Spline(
                sp.getPoint(startPadding),
                sp.getPoint(startPadding) + sp.getDirection(startPadding),
                sp.getPoint(length - endPadding),
                sp.getPoint(length - endPadding) + sp.getDirection(length - endPadding)
            )
        }

        TODO("multisplinepart")
    }


    class SplinePart(val x: Poly3, val y: Poly3, var offset: Double, val length: Double, val normalized: Boolean) {
        private val endDist = if (normalized) 1.0 else length
        private val stepSize = 0.2
        private val normalizedPositions = mutableListOf<Double>()

        init {
            // 0.0 <= f(distance) <= 1.0
            // such that length of spline until f(distance) = distance
            // f(0) = 0
            // f'(0) = 1/|(x'(0), y'(0))|
            // Compensate polynom's derivative length

            val iterationCount = floor(length / stepSize).toInt()
            var value = 0.0
            for (i in 0..iterationCount) {
                normalizedPositions.add(value)
                val directionLen = Vec2(x.derivativeValue(value), y.derivativeValue(value)).length()
                value += stepSize / directionLen
            }
        }

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
                val t = getNormalizedPosition(distance)
                return Vec2(x.value(t), y.value(t))
            }
            return Vec2(x.value(distance), y.value(distance))
        }

        fun getDirection(distance: Double): Vec2 {
            if (normalized) {
                val t = getNormalizedPosition(distance)
                return Vec2(x.derivativeValue(t), y.derivativeValue(t))
            }
            return Vec2(x.derivativeValue(distance), y.derivativeValue(distance))
        }

        // Point, direction
        fun closestPoint(point: Vec2): Pair<Vec2, Vec2> {
            val bestGuess = getGuessClosestToPoint(point)

            return Pair(
                Vec2(x.value(bestGuess), y.value(bestGuess)),
                Vec2(x.derivativeValue(bestGuess), y.derivativeValue(bestGuess))
            )
        }

        fun closestPointWithDistance(point: Vec2): Triple<Vec2, Vec2, Double> {
            val bestGuess = getGuessClosestToPoint(point)

            return Triple(
                Vec2(x.value(bestGuess), y.value(bestGuess)),
                Vec2(x.derivativeValue(bestGuess), y.derivativeValue(bestGuess)),
                if (normalized) { calculateLength(x, y, bestGuess) } else { bestGuess }
            )
        }

        private fun getGuessClosestToPoint(point: Vec2): Double {
            val maxValue = endDist
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
            return bestGuess
        }

        private fun getNormalizedPosition(distance: Double): Double {
            val stepNumber = floor(distance / stepSize).toInt()
            val base = normalizedPositions[stepNumber]
            val rest = distance - stepNumber * stepSize
            val directionLen = Vec2(x.derivativeValue(base), y.derivativeValue(base)).length()
            val value = base + rest / directionLen
            return value
        }

        override fun toString(): String {
            return "SplinePart((${x.a} + ${x.b}*t + ${x.c}*t^2 + ${x.d}*t^3, ${y.a} + ${y.b}*t + ${y.c}*t^2 + ${y.d}*t^3), length=$length, normalized=$normalized offset=$offset)"
        }
    }

    override fun toString(): String {
        var result = "Spline(length=$length, parts=["
        for (sp in splineParts) {
            result += "\n$sp,"
        }
        result += "\n])\n"
        return result
    }
}

// Must be: 0.0 <= end <= 1.0
fun calculateLength(x: Poly3, y: Poly3, end: Double = 1.0): Double {
    val iterations = 1000
    var length = 0.0
    var prev = Vec2(x.value(0.0), y.value(0.0))
    val step = end / iterations
    for (i in 0..iterations) {
        val cur = Vec2(x.value(step * i), y.value(step * i))
        length += prev.distance(cur)
        prev = cur
    }
    return length
}

fun main() {
    val spline = Spline()
    spline.addSpiral(Vec2(0.0, 0.0), 0.0, 0.1, 2.0, 5.0)
    println(spline)
    println(spline.getPoint(spline.length / 2.0))
    println(spline.getDirection(spline.length / 2.0))

//    val spline2 = spline.copy(spline.length / 2.0)

//    println("dir= ${spline.getDirection(spline.length)}")
//    println("dir= ${spline2.getDirection(spline2.length)}")
//    println(spline2)
}
