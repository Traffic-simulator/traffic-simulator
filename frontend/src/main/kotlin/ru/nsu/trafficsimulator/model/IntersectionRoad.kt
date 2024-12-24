package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Spline
import kotlin.math.abs
import kotlin.math.min

data class IntersectionRoad(
    val id: Long,
    val intersection: Intersection,
    val fromRoad: Road,
    val toRoad: Road,
    val lane: Int = 1,
    var geometry: Spline
) {
    val laneLinkage: MutableList<Triple<Int, Int, Int>> = mutableListOf()

    override fun toString(): String {
        return "Road(id=$id, geometry=$geometry), laneLinkage=$laneLinkage"
    }

    fun recalculateGeometry() {
        val incomingLaneNumber = fromRoad.getIncomingLaneNumber(intersection)
        val outgoingLaneNumber = toRoad.getOutgoingLaneNumber(intersection)
        val laneNumber =
            min(abs(incomingLaneNumber), abs(outgoingLaneNumber))

        val dirLength1 = fromRoad.getIntersectionPoint(intersection).distance(intersection.position)
        val dirLength2 = toRoad.getIntersectionPoint(intersection).distance(intersection.position)
        val geometry = Spline(
            fromRoad.getIntersectionPoint(intersection, laneNumber - abs(incomingLaneNumber)).xzProjection(),
            fromRoad.getIntersectionPoint(intersection, laneNumber - abs(incomingLaneNumber)).xzProjection() + fromRoad.getIntersectionDirection(intersection, true).xzProjection().setLength(dirLength1),
            toRoad.getIntersectionPoint(intersection, abs(outgoingLaneNumber) - laneNumber).xzProjection(),
            toRoad.getIntersectionPoint(intersection, abs(outgoingLaneNumber) - laneNumber).xzProjection() + toRoad.getIntersectionDirection(intersection, false).xzProjection().setLength(dirLength2))

        this.geometry = geometry
    }
}
