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
        when (getContact(intersection)) {
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
        when (getContact(intersection)) {
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


    private fun getContact(intersection: Intersection): ContactPoint {
        if (intersection === startIntersection) return ContactPoint.START
        if (intersection === endIntersection) return ContactPoint.END
        return ContactPoint.NULL
    }


    companion object {
        private enum class ContactPoint {
            START, END, NULL
        }
    }

    override fun toString(): String {
        return "Road(id=$id, start=${startIntersection?.id}, end=${endIntersection?.id}, spline=${geometry})"
    }
}

