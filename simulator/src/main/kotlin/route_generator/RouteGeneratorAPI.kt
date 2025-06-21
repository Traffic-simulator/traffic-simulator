package route_generator

import network.Waypoint

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
}
