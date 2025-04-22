package route_generator

import Waypoint

// TODO: replace with buildings spawner logic. API will stay the same.
interface IRouteGenerator {
    fun update(dt: Double, create: VehicleCreationListener, isPositionFree: WaypointSpawnAbilityChecker)
}

interface RouteGeneratorDespawnListener {
    fun onDespawn(vehicleId: Int);
}

interface VehicleCreationListener {
    fun createVehicle(source: Waypoint, destination: Waypoint, onDespawn: RouteGeneratorDespawnListener): Int?
}

interface WaypointSpawnAbilityChecker {
    fun isFree(waypoint: Waypoint): Boolean
}
