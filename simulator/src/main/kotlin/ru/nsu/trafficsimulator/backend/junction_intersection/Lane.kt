package ru.nsu.trafficsimulator.backend.junction_intersection

import kotlin.math.absoluteValue


class Lane (
    val referenceLine: Spline,
    val id: Long
) {
    companion object {
        val EPSILON = 0.1
        val OFFSET : Double = 4.0
        val NUMBER_OF_SECTIONS = 100
    }
    val leftPoints: MutableList<Pair<Double, Double>> = ArrayList()
    val rightPoints: MutableList<Pair<Double, Double>> = ArrayList()
    init {
        repeat(NUMBER_OF_SECTIONS + 1) { i ->  // 101 итерация, чтобы включить 1.0
            val p = i /  NUMBER_OF_SECTIONS.toDouble()
            var derivative = referenceLine.valueOfDerivative(p)
            var perpendikular : Pair<Double, Double>;
            if (id < 0) {
                perpendikular = referenceLine.getPerpendicularRight(derivative)
            } else if (id > 0) {
                perpendikular = referenceLine.getPerpendicularLeft(derivative)
            } else {
                throw IllegalArgumentException()
            }
            var normilizedPerpendikular = referenceLine.normalizeVector(perpendikular);
            var referencePoint : Pair<Double, Double> = referenceLine.valueOfGlobal(p)
            var addOffset1 = (id.absoluteValue - 1) * OFFSET + EPSILON
            var addOffset2 = (id.absoluteValue) * OFFSET - EPSILON
            leftPoints.add(Pair(
                referencePoint.first + addOffset1 * normilizedPerpendikular.first,
                referencePoint.second + addOffset1 * normilizedPerpendikular.second))
            rightPoints.add(Pair(
                referencePoint.first + addOffset2 * normilizedPerpendikular.first,
                referencePoint.second + addOffset2 * normilizedPerpendikular.second
            ))

        }
    }

}
