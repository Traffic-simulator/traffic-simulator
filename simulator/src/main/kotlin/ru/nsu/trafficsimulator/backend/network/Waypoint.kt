package ru.nsu.trafficsimulator.backend.network

import ru.nsu.trafficsimulator.backend.path.Path

class Waypoint (
    val roadId: String,
    val laneId: String,
    ) {

    companion object {

        fun fromPathWaypointNullable(pathWaypoint: Path.PathWaypoint?): Waypoint? =
            pathWaypoint?.let { Waypoint(it.lane.roadId, it.lane.laneId.toString()) }

        fun fromPathWaypoint(pathWaypoint: Path.PathWaypoint): Waypoint =
            Waypoint(pathWaypoint.lane.roadId, pathWaypoint.lane.laneId.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Waypoint) return false

        return roadId == other.roadId && laneId == other.laneId
    }

    override fun hashCode(): Int {
        var result = roadId.hashCode()
        result = 31 * result + laneId.hashCode()
        return result
    }

    override fun toString(): String = "Waypoint(roadId='$roadId', laneId='$laneId')"
}

