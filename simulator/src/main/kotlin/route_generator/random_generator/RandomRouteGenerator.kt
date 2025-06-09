package route_generator.random_generator

import route_generator.IRouteGenerator
import route_generator.RouteGeneratorDespawnListener
import route_generator.VehicleCreationListener
import route_generator.WaypointSpawnAbilityChecker
import route_generator_new.discrete_function.Building
import kotlin.random.Random

class RandomRouteGenerator(val rnd: Random, val buildings: List<Building>):
    IRouteGenerator {
    val timer = 0.1
    var spawnTimer = timer

    override fun update(dt: Double, create: VehicleCreationListener, waypointChecker: WaypointSpawnAbilityChecker) {
        spawnTimer += dt
        if (spawnTimer < timer) return

        val spawnDetails = buildings.map { create.getWaypointByJunction(it.junctionId, true) }
        val despawnDetails = buildings.map { create.getWaypointByJunction(it.junctionId, false)}

        val availablePositions = spawnDetails.filter { waypointChecker.isFree(it) }
        if (availablePositions.isEmpty()) return
        val fromIdx = rnd.nextInt(availablePositions.size)
        val toIdx = rnd.nextInt(despawnDetails.size)
        if (!availablePositions[fromIdx].roadId.equals(despawnDetails[toIdx].roadId)) {
            val id = create.createVehicle(availablePositions[fromIdx], despawnDetails[toIdx], onDespawn)
        }

        spawnTimer = 0.0
    }

    private val onDespawn = object : RouteGeneratorDespawnListener {
        override fun onDespawn(vehicleId: Int) {
            // In random generator it's not needed
        }
    }
}
