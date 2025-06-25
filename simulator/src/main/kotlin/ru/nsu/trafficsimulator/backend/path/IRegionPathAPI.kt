package ru.nsu.trafficsimulator.backend.path

import ru.nsu.trafficsimulator.backend.network.Waypoint

interface IRegionPathAPI {
    fun isRegionRoad(roadId: Int, regionId: Int): Boolean
    fun isRegionRoad(roadId: String, regionId: Int): Boolean
    fun getGlobalPath(source: Waypoint, destination: Waypoint): Pair<Double, List<Path.PathWaypoint>>
    fun getPathTime(path: List<Path.PathWaypoint>, beforeExcluded: Path.PathWaypoint, time: Double): Double
    fun getWaypointAvgSpeed(waypoint: Waypoint, time: Double): Double
}
