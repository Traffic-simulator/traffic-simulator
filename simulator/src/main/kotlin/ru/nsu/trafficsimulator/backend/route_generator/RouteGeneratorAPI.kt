package ru.nsu.trafficsimulator.backend.route_generator

import ru.nsu.trafficsimulator.backend.network.Waypoint

interface IRouteGenerator {
    fun update(dt: Double, create: VehicleCreationListener, isPositionFree: WaypointSpawnAbilityChecker)
}

interface RouteGeneratorDespawnListener {
    fun onDespawn(vehicleId: Int);
}

interface VehicleCreationListener {
    fun createVehicle(source: Waypoint, destination: Waypoint, onDespawn: RouteGeneratorDespawnListener): Int?
    fun getWaypointByJunction(junctionId: String, isStart: Boolean): Waypoint
}

interface WaypointSpawnAbilityChecker {
    fun isFree(waypoint: Waypoint): Boolean
    fun isFreeWithSpeed(source: Waypoint, destination:Waypoint, speed: Double): Boolean
}
