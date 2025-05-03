package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Spline
import kotlin.math.abs

data class IntersectionRoad(
    val id: Long,
    val intersection: Intersection,
    val fromRoad: Road,
    val toRoad: Road,
    var geometry: Spline,
    val laneLinkage: Pair<Int, Int>
) {
    val lane get() = 1

    override fun toString(): String {
        return "Road(id=$id, geometry=$geometry), laneLinkage=$laneLinkage"
    }

    fun recalculateGeometry() {
        val dirLength1 = fromRoad.getIntersectionPoint(intersection).distance(intersection.position.toVec3())
        val dirLength2 = toRoad.getIntersectionPoint(intersection).distance(intersection.position.toVec3())
        val geometry = Spline(
            fromRoad.getIntersectionPoint(intersection, abs(laneLinkage.first) - 1).xzProjection(),
            fromRoad.getIntersectionPoint(intersection, abs(laneLinkage.first) - 1)
                .xzProjection() + fromRoad.getIntersectionDirection(intersection, true).xzProjection()
                .setLength(dirLength1),
            toRoad.getIntersectionPoint(intersection, -abs(laneLinkage.second) + 1).xzProjection(),
            toRoad.getIntersectionPoint(intersection, -abs(laneLinkage.second) + 1)
                .xzProjection() + toRoad.getIntersectionDirection(intersection, false).xzProjection()
                .setLength(dirLength2)
        )

        this.geometry = geometry
    }
}
