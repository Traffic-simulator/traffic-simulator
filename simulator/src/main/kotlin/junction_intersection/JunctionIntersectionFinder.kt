package junction_intersection

import network.junction.Junction
import network.Network
import network.Road
import kotlin.math.pow

class JunctionIntersectionFinder(private val network: Network) {
    companion object {
        private val NUMBER_OF_SECTIONS_IN_SPLINES = 100
    }
    private var allRoads : List<Road> = network.roads;
    private var allRoadsMap : MutableMap<String, Road> = HashMap()
    init {
    	for (road in allRoads) {
            allRoadsMap[road.id] = road
        }
    }
    fun findIntersection (junction : Junction) {
        var indexesOfRoadsInJunction : ArrayList<String> = ArrayList<String>()
        var junctionRoads : List<Road>
        for (connection in junction.tjunction.connection) {
            indexesOfRoadsInJunction.add(connection.connectingRoad);
        }
    }


    //return list of intersection points
    private fun twoSplinesIntersection (
        firstSpline: Spline,
        secondSpline: Spline
    ): List<Pair<Double, Double>>{
        var first = firstSpline
        if (firstSpline.pRange == PRange.ARC_LENGTH) {
            first = normalizingSpline(firstSpline)
        }
        var second = secondSpline
        if (secondSpline.pRange == PRange.ARC_LENGTH) {
            second = normalizingSpline(secondSpline)
        }
        //splines are normalized

        val listFirst = mutableListOf<Pair<Double, Double>>()
        val listSecond = mutableListOf<Pair<Double, Double>>()


        repeat(NUMBER_OF_SECTIONS_IN_SPLINES + 1) { i ->  // 101 итерация, чтобы включить 1.0
            val p = i /  NUMBER_OF_SECTIONS_IN_SPLINES.toDouble()
            listFirst.add(valueOfNormalizedSpline(firstSpline, p))
            listSecond.add(valueOfNormalizedSpline(secondSpline, p))
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

    //all splines will be used in normalized form
    private fun normalizingSpline(spline: Spline): Spline {
        if (spline.pRange == PRange.NORMALIZED) {
            return spline
        }
        return Spline(
            spline.aU,
            spline.bU * spline.length,
            spline.cU * spline.length.pow(2),
            spline.dU * spline.length.pow(3),
            spline.aV,
            spline.bV * spline.length,
            spline.cV * spline.length.pow(2),
            spline.dV * spline.length.pow(3),
            PRange.NORMALIZED,
            1.0
        )
    }

    //direction of U(V) is equals the axis X(Y) (This say Rustam :) )
    private fun valueOfNormalizedSpline(
        spline: Spline,
        p: Double
    ) : Pair<Double, Double> {
        if (p < 0 || p > 1.0) {
            throw IllegalArgumentException("parameter p is should be between 0 and 1.")
        }
        val x = spline.aU + spline.bU * p + spline.cU * p + spline.dU * p;
        val y = spline.aV + spline.bV * p + spline.cV * p + spline.dV * p;
        return Pair(x, y)
    }

}
