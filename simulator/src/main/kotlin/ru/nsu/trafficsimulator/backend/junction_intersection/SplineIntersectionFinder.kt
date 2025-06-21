package ru.nsu.trafficsimulator.backend.junction_intersection

interface SplineIntersectionFinder {
    fun twoSplinesIntersection (
        firstSpline: Spline,
        secondSpline: Spline
    ): List<Pair<Double, Double>>
}
