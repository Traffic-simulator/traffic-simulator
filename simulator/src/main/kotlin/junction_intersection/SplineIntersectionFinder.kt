package junction_intersection

interface SplineIntersectionFinder {
    fun twoSplinesIntersection (
        firstSpline: Spline,
        secondSpline: Spline
    ): List<Pair<Double, Double>>
}
