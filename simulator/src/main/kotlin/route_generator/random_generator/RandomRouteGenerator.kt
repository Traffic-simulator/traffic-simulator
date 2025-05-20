package route_generator.random_generator

import Waypoint
import route_generator.IRouteGenerator
import route_generator.RouteGeneratorDespawnListener
import route_generator.VehicleCreationListener
import route_generator.WaypointSpawnAbilityChecker
import kotlin.random.Random

class RandomRouteGenerator(val rnd: Random, val spawnDetails: ArrayList<Waypoint>, val despawnDetails: ArrayList<Waypoint>):
    IRouteGenerator {
    val timer = 0.1
    var spawnTimer = timer

    override fun update(dt: Double, creator: VehicleCreationListener, waypointChecker: WaypointSpawnAbilityChecker) {
        spawnTimer += dt
        if (spawnTimer < timer) return

        val availablePositions = spawnDetails.filter { waypointChecker.isFree(it) }
        if (availablePositions.isEmpty()) return
        val fromIdx = rnd.nextInt(availablePositions.size)
        val toIdx = rnd.nextInt(despawnDetails.size)
        val id = creator.createVehicle(availablePositions[fromIdx], despawnDetails[toIdx], onDespawn)

        spawnTimer = 0.0
    }

    private val onDespawn = object : RouteGeneratorDespawnListener {
        override fun onDespawn(vehicleId: Int) {
            // In random generator it's not needed
        }
    }
}
