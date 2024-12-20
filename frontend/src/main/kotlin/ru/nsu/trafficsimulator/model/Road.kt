package ru.nsu.trafficsimulator.model

class Road(
    val id: Long,
    var startIntersection: Intersection?,
    var endIntersection: Intersection?,
    var leftLane: Int = 1,
    var rightLane: Int = 1,
    var geometry: Spline
) {
    val startPadding
        get() = startIntersection?.padding ?: 0.0

    val endPadding
        get() = endIntersection?.padding ?: 0.0

    val length
        get() = geometry.length - startPadding - endPadding

    fun getPoint(distance: Double): Vec3 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("distance must be between 0 and length")
        }
        return geometry.getPoint(startPadding + distance).toVec3()
    }

    fun getDirection(distance: Double): Vec3 {
        if (distance < 0 || distance > length) {
            throw IllegalArgumentException("distance must be between 0 and length")
        }
        return geometry.getDirection(startPadding + distance).toVec3()
    }

    // laneOffset < 0 - lanes incoming to the intersection
    // laneOffset > 0 - lanes outgoing from the intersection
    fun getIntersectionPoint(intersection: Intersection, laneOffset: Int = 0): Vec3 {
        return when (contact(intersection)) {
            ContactPoint.START -> {
                val dir = getDirection(0.0)
                val normDir = Vec3(dir.x, 0.0, dir.z).normalized()
                getPoint(0.0) + Vec3(normDir.z, normDir.y, -normDir.x) * (Layout.LANE_WIDTH * laneOffset)
            }

            ContactPoint.END -> {
                val dir = getDirection(length)
                val normDir = Vec3(dir.x, 0.0, dir.z).normalized()
                getPoint(length) + Vec3(-normDir.z, normDir.y, normDir.x) * (Layout.LANE_WIDTH * laneOffset)
            }

            ContactPoint.NULL -> throw IllegalArgumentException("Invalid intersection")
        }
    }

    fun moveRoad(intersection: Intersection, newPosition: Vec3) {
        when (contact(intersection)) {
            ContactPoint.START -> {
                val (point, dir) = geometry.splineParts.first().getStartPoint()
                val tangent = dir - point
                geometry.moveStart(newPosition.xzProjection(), newPosition.xzProjection() + tangent)
            }

            ContactPoint.END -> {
                val (point, dir) = geometry.splineParts.last().getEndPoint()
                val tangent = dir - point
                geometry.moveEnd(newPosition.xzProjection(), newPosition.xzProjection() + tangent)
            }

            ContactPoint.NULL -> throw IllegalArgumentException("Invalid intersection")
        }
    }

    fun redirectRoad(intersection: Intersection, newDirection: Vec3) {
        when (contact(intersection)) {
            ContactPoint.START -> {
                val point = geometry.splineParts.first().getStartPoint().first
                geometry.moveStart(point, newDirection.xzProjection())
            }

            ContactPoint.END -> {
                val point = geometry.splineParts.last().getEndPoint().first
                geometry.moveEnd(point, newDirection.xzProjection())
            }

            ContactPoint.NULL -> throw IllegalArgumentException("Invalid intersection")
        }
    }

    fun getIncomingLaneNumber(intersection: Intersection): Int {
        return when (contact(intersection)) {
            ContactPoint.START -> leftLane
            ContactPoint.END -> -rightLane
            ContactPoint.NULL -> throw IllegalArgumentException("Invalid intersection")
        }
    }

    fun getOutgoingLaneNumber(intersection: Intersection): Int {
        return when (contact(intersection)) {
            ContactPoint.START -> -rightLane
            ContactPoint.END -> leftLane
            ContactPoint.NULL -> throw IllegalArgumentException("Invalid intersection")
        }
    }

    private fun contact(intersection: Intersection): ContactPoint {
        if (intersection === startIntersection) return ContactPoint.START
        if (intersection === endIntersection) return ContactPoint.END
        return ContactPoint.NULL
    }

    companion object {
        private enum class ContactPoint {
            START, END, NULL
        }
    }
}

