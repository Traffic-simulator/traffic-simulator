package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Spline
import kotlin.math.abs

data class IntersectionRoad(
    var id: Long,
    val intersection: Intersection,
    val fromRoad: Road,
    val toRoad: Road, val laneLinkage: Pair<Int, Int>, var geometry: Spline
) {
    val lane get() = 1

    override fun toString(): String {
        return "Road(id=$id, geometry=$geometry), laneLinkage=$laneLinkage"
    }

    fun recalculateGeometry() {
        this.geometry = calculateGeometry()
    }

    private fun calculateGeometry(): Spline {
        val dirLength1 = fromRoad.getIntersectionPoint(intersection).distance(intersection.position.toVec3())
        val dirLength2 = toRoad.getIntersectionPoint(intersection).distance(intersection.position.toVec3())
        return Spline(
            fromRoad.getIntersectionPoint(intersection, abs(laneLinkage.first) - 1).xzProjection(),
            fromRoad.getIntersectionPoint(intersection, abs(laneLinkage.first) - 1)
                .xzProjection() + fromRoad.getIntersectionDirection(intersection, true).xzProjection()
                .setLength(dirLength1),
            toRoad.getIntersectionPoint(intersection, -abs(laneLinkage.second) + 1).xzProjection(),
            toRoad.getIntersectionPoint(intersection, -abs(laneLinkage.second) + 1)
                .xzProjection() + toRoad.getIntersectionDirection(intersection, false).xzProjection()
                .setLength(dirLength2)
        )
    }
}
