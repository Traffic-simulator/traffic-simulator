package ru.nsu.trafficsimulator.model

class Road(
    val id: Long,
    var startIntersection: Intersection?,
    var endIntersection: Intersection?,
    var leftLane: Int = 1,
    var rightLane: Int = 1,
    var geometry: Spline
) {
    fun moveRoad(intersection: Intersection, newPosition: Vec3) {
        if (startIntersection != null && startIntersection === intersection) {
            val (point, dir) = geometry.splineParts.first().getStartPoint()
            val tangent = dir - point
            geometry.moveStart(newPosition.xzProjection(), newPosition.xzProjection() + tangent)

        } else if (endIntersection != null && endIntersection === intersection) {
            val (point, dir) = geometry.splineParts.last().getEndPoint()
            val tangent = dir - point
            geometry.moveEnd(newPosition.xzProjection(), newPosition.xzProjection() + tangent)
        } else {
            throw IllegalArgumentException("Invalid intersection")
        }
    }

    override fun toString(): String {
        return "Road(id=$id, start=${startIntersection?.id}, end=${endIntersection?.id}, spline=${geometry})"
    }
}
