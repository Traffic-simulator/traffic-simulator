package ru.nsu.trafficsimulator.backend.junction_intersection


import kotlin.math.pow

class SplineIntersectionFinderSectionImpl(
    private val numberOfSectionsInSpline: Int = 100
) : SplineIntersectionFinder {

    //return list of intersection points
    override fun twoSplinesIntersection (
        firstSpline: Spline, //first spline
        secondSpline: Spline //second spline
    ): List<Pair<Double, Double>> {
        var first = firstSpline
//        if (firstSpline.pRange == EParamPoly3PRange.NORMALIZED) {
//            first = normalizingSpline(firstSpline)
//        }
        var second = secondSpline
//        if (secondSpline.pRange == EParamPoly3PRange.ARC_LENGTH) {
//            second = normalizingSpline(secondSpline)
//        }
        //splines are normalized

        val listFirst = mutableListOf<Pair<Double, Double>>()
        val listSecond = mutableListOf<Pair<Double, Double>>()


        repeat(numberOfSectionsInSpline + 1) { i ->  // 101 итерация, чтобы включить 1.0
            val p = i /  numberOfSectionsInSpline.toDouble()
            listFirst.add(valueOfNormalizedSpline(first, p))
            listSecond.add(valueOfNormalizedSpline(second, p))
        }
        //have two lists of spline sections

        val listOfIntersections = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until listFirst.size - 1) {
            val firstLine = Pair(listFirst[i], listFirst[i + 1])
            for (j in 0 until listSecond.size - 1) {
                val secondLine = Pair(listSecond[j], listSecond[j + 1])
                val result = findSectionIntersection(firstLine, secondLine)
                result?.let {
                    listOfIntersections.add(result)
                }
            }
        }

        return listOfIntersections
    }

    //for two straight lines(sections)
    //return null if solution isn't exist
    private fun findSectionIntersection(
        line1: Pair<Pair<Double, Double>, Pair<Double, Double>>, // (x1, y1) to (x2, y2)
        line2: Pair<Pair<Double, Double>, Pair<Double, Double>>  // (x3, y3) to (x4, y4)
    ): Pair<Double, Double>? {
        val (x1, y1) = line1.first
        val (x2, y2) = line1.second
        val (x3, y3) = line2.first
        val (x4, y4) = line2.second

        //maybe Cramer)
        val denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (denominator == 0.0) return null // if lines are parallel or equal
        val px = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denominator
        val py = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denominator

        // check that point belongs to lines
        val withinLine1 = px in minOf(x1, x2)..maxOf(x1, x2) && py in minOf(y1, y2)..maxOf(y1, y2)
        val withinLine2 = px in minOf(x3, x4)..maxOf(x3, x4) && py in minOf(y3, y4)..maxOf(y3, y4)

        return if (withinLine1 && withinLine2) Pair(px, py) else null
    }


    //direction of U(V) is equals the axis X(Y) (This say Rustam :) )
    private fun valueOfNormalizedSpline(
        spline: Spline,
        p: Double
    ) : Pair<Double, Double> {
        if (p < 0 || p > 1.0) {
            throw IllegalArgumentException("parameter p is should be between 0 and 1.")
        }
        val x = spline.aU + spline.bU * p + spline.cU * p.pow(2)  + spline.dU * p.pow(3);
        val y = spline.aV + spline.bV * p + spline.cV * p.pow(2) + spline.dV * p.pow(3);
        return Pair(x, y)
    }
}
