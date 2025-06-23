package ru.nsu.trafficsimulator.backend.route_generator

import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.Path

interface IRouteGenerator {
    fun update(dt: Double, create: VehicleCreationListener, isPositionFree: WaypointSpawnAbilityChecker)
}

interface IRegionAPI {
    fun isRegionRoad(roadId: Int, regionId: Int): Boolean
    fun isRegionRoad(roadId: String, regionId: Int): Boolean
    fun getGlobalPath(source: Waypoint, destination: Waypoint): Pair<Double, List<Path.PathWaypoint>>

    fun getPathTime(path: List<Path.PathWaypoint>, beforeExcluded: Path.PathWaypoint): Double
    fun getWaypointAvgSpeed(waypoint: Waypoint): Double
}

interface RouteGeneratorDespawnListener {
    fun onDespawn(vehicleId: Int);
}

interface VehicleCreationListener {
    // Assume that source and destination is connected TODO: add checker for it
    fun createVehicle(source: Waypoint, destination: Waypoint, onDespawn: RouteGeneratorDespawnListener, initialSpeed: Double = 0.0 ): Int
    fun getWaypointByJunction(junctionId: String, isStart: Boolean): Waypoint
}

interface WaypointSpawnAbilityChecker {
    fun isFree(waypoint: Waypoint): Boolean
    fun isFreeWithSpeed(source: Waypoint, destination:Waypoint, speed: Double): Boolean
}

