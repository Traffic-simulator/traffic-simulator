package ru.nsu.trafficsimulator.model

import java.util.*
import kotlin.math.*

class Spline(start: SplineVertex, end: SplineVertex) {
    val splineParts: MutableList<SplinePart>
    var length: Double

    init {
        this.splineParts = LinkedList()
        this.length = 0.0

        addSplinePart(start, end)
    }

    fun getPoint(distance: Double): Point2 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("Offset must be between 0 and length")
        }

        var sp: SplinePart = splineParts.first()
        for (splinePart in splineParts) {
            if (splinePart.offset < distance) {
                sp = splinePart
            } else {
                break
            }
        }

        return sp.getPoint(distance - sp.offset)
    }


    fun addPoint(newPoint: SplineVertex) {
        val prevPoint = splineParts.last().end
        addSplinePart(prevPoint, newPoint)
    }

    fun addSplinePart(start: SplineVertex, end: SplineVertex) {
        val (x, y) = normalizedPolynom(start, end)
        val partLength = calculateLength(x, y)
        splineParts.add(SplinePart(x, y, this.length, partLength, true))
        this.length += partLength
    }


    fun addLane(start: Point2, angle: Double, length: Double) {
        val startVertex = SplineVertex(start, start + Point2(cos(angle), sin(angle)))
        val endVertex = SplineVertex(
            start + Point2(cos(angle), sin(angle)) * length,
            start + Point2(cos(angle), sin(angle)) * (length + 1)
        )
        addSplinePart(startVertex, endVertex)
    }


    fun addPoly(
        start: Point2,
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

    fun addArc(start: Point2, startAngle: Double, curvature: Double, length: Double) {
        val maxPart = PI / 2.0

        val deltaAngle = curvature * length

        val parts = ceil(abs(deltaAngle) / maxPart).toInt()
        val step = deltaAngle / parts

        val r = 1 / curvature

        var curPoint = start
        var curAngle = startAngle
        for (i in 0 until parts) {
            val endAngle = curAngle + step

            val endPoint = curPoint - Point2(
                sin(curAngle) - sin(endAngle),
                -cos(curAngle) + cos(endAngle)
            ) * r

            addSplinePart(
                SplineVertex(curPoint, curPoint + Point2(cos(curAngle), sin(curAngle)) * (length / parts)),
                SplineVertex(endPoint, endPoint + Point2(cos(endAngle), sin(endAngle)) * (length / parts)))

            curPoint = endPoint
            curAngle = endAngle
        }
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


    private fun normalizedPolynom(start: SplineVertex, end: SplineVertex): Pair<Poly3, Poly3> {
        val x = Poly3(
            start.position.x,
            start.direction.x - start.position.x,
            4 * end.position.x - 2 * start.direction.x - start.position.x - end.direction.x,
            end.direction.x + start.position.x + start.direction.x - 3 * end.position.x
        )

        val y = Poly3(
            start.position.y,
            start.direction.y - start.position.y,
            4 * end.position.y - 2 * start.direction.y - start.position.y - end.direction.y,
            end.direction.y + start.position.y + start.direction.y - 3 * end.position.y
        )
        return (x to y)
    }

    companion object {

//        fun ofLane(start: Point2, angle: Double, length: Double): Spline {
//            val direction = Point2(cos(angle), sin(angle)) // union length
//            val end = start + direction * length
//            return Spline(start, start + direction, end, end + direction)
//        }
//
//        fun ofArc(start: Point2, startAngle: Double, curvature: Double, length: Double): Spline {
//            val r = 1 / curvature
//            val endAngle = startAngle + curvature * length
//            val end = start - Point2(
//                sin(startAngle) - sin(endAngle),
//                -cos(startAngle) + cos(endAngle)
//            ) * r
//            return Spline(
//                start,
//                start + Point2(cos(startAngle), sin(startAngle)) * length,
//                end,
//                end + Point2(cos(endAngle), sin(endAngle)) * length
//            )
//        }


    }
}

class SplinePart(val x: Poly3, val y: Poly3, val offset: Double, val length: Double, val normalized: Boolean) {
    private val endDist = if (normalized) 1.0 else length

    val start: SplineVertex by lazy {
        SplineVertex(
            Point2(x.value(0.0), y.value(0.0)),
            Point2(x.value(0.0), y.value(0.0)) + Point2(x.derivativeValue(0.0), y.derivativeValue(0.0))
        )
    }

    val end: SplineVertex by lazy {
        SplineVertex(
            Point2(x.value(endDist), y.value(endDist)),
            Point2(x.value(endDist), y.value(endDist)) + Point2(x.derivativeValue(endDist), y.derivativeValue(endDist))
        )
    }

    fun getPoint(distance: Double): Point2 {
        if (normalized) {
            return Point2(x.derivativeValue(distance / length), y.derivativeValue(distance / length))
        }
        return Point2(x.value(distance), y.value(distance))
    }
}

data class SplineVertex(val position: Point2, val direction: Point2)

fun main() {

    val spline =
        Spline(SplineVertex(Point2(0.0, 0.0), Point2(0.0, 5.0)), SplineVertex(Point2(2.0, 2.0), Point2(6.0, 2.0)))
    spline.addLane(Point2(2.0, 2.0), 0.0, 4.0)
    spline.addArc(Point2(6.0, 2.0), 0.0, 1.0, 6 * PI / 4)
    spline.addArc(Point2(5.0, 3.0), 3 * PI / 2.0, -2.0,  PI / 2)
    spline.addPoint(SplineVertex(Point2(3.0, 3.0), Point2(1.0, 3.0)))


    for (sp in spline.splineParts) {
        println(Poly3toString(sp.x, sp.y))
        println(sp.length)
        println()
    }
}

fun Poly3toString(p1: Poly3, p2: Poly3): String {
    return "(${p1.a} + ${p1.b} * t + ${p1.c} * t^2 + ${p1.d} * t^3,${p2.a} + ${p2.b} * t + ${p2.c} * t^2 + ${p2.d} * t^3)"
}

