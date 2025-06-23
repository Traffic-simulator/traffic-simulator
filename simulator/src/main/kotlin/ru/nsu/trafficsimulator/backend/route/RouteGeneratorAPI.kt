package ru.nsu.trafficsimulator.backend.route

import ru.nsu.trafficsimulator.backend.network.Waypoint

interface IRouteGenerator {
    fun update(dt: Double, create: VehicleCreationListener, isPositionFree: WaypointSpawnAbilityChecker)
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

