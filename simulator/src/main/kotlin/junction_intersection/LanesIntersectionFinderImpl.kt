package junction_intersection

class LanesIntersectionFinderImpl : LanesIntersectionFinder {
    override fun findIntersections(lanes1: Lanes, lanes2: Lanes): MutableList<Intersection> {
        var intersections = mutableListOf<Intersection>()
        for (lane1 in lanes1.negativeLanes) {
            val lane1left = lane1.leftPoints
            val lane1right = lane1.rightPoints
            for (lane2 in lanes2.negativeLanes) {
                val lane2left = lane2.leftPoints
                val lane2right = lane2.rightPoints
                if (
                    areTwoSplineIntersect(lane1left, lane2left)
                    || areTwoSplineIntersect(lane1left, lane2right)
                    || areTwoSplineIntersect(lane1right, lane2left)
                    || areTwoSplineIntersect(lane1right, lane2right)
                ) {
                    intersections.add(Intersection(lanes1.roadId, lane1.id, lanes2.roadId, lane2.id))
                }
            }
        }
        return intersections
    }

    private fun areTwoSplineIntersect(
        spline1:MutableList<Pair<Double, Double>>,
        spline2:MutableList<Pair<Double, Double>>
    ) : Boolean {
        for (i in 0 until spline1.size - 1) {
            val firstLine = Pair(spline1[i], spline1[i + 1])
            for (j in 0 until spline2.size - 1) {
                val secondLine = Pair(spline2[j], spline2[j + 1])
                val result = findSectionIntersection(firstLine, secondLine)
                if (result != null) {
                    return true
                }
            }
        }
        return false
    }



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
}
