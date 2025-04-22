import junction_intersection.JunctionIntersectionFinder
import network.Network
import opendrive.OpenDRIVE
import route_generator.IRouteGenerator
import route_generator.RouteGeneratorDespawnListener
import route_generator.VehicleCreationListener
import route_generator.WaypointSpawnAbilityChecker
import route_generator.random_generator.RandomRouteGenerator
import vehicle.Vehicle
import vehicle.model.MOBIL
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.random.Random

// Route - source point and destination point.
// Path - all concrete roads and lanes that vehicle will go
class Simulator(openDrive: OpenDRIVE, val spawnDetails: ArrayList<Waypoint>, val despawnDetails: ArrayList<Waypoint>, seed: Long) {

    val finder = JunctionIntersectionFinder(openDrive)
    val intersections = finder.findIntersection()
    val network: Network = Network(openDrive.road, openDrive.junction, intersections)
    val rnd = Random(seed)
    val routeGeneratorAPI: IRouteGenerator = RandomRouteGenerator(rnd, spawnDetails, despawnDetails)

    val vehicles: ArrayList<Vehicle> = ArrayList()

    // TODO: Correct lane id checking
    // TODO: staged updates
    fun update(dt: Double): ArrayList<Vehicle> {

        processNonmandatoryLaneChanges()

        val sortedVehicles = vehicles.sortedBy   { it.position }.reversed()

        // Stage y:
        sortedVehicles.forEach { it ->
            it.update(dt)
        }

        // despawn vehicles
        vehicles.removeAll { it.despawned == true}

        // spawn new vehicles
        routeGeneratorAPI.update(dt, createVehicle, isPositionFree)

        return vehicles
    }

    fun processNonmandatoryLaneChanges() {
        // Stage x: non-mandatory lane changes
        vehicles.forEach { it ->
            // TODO: Currently lane changes on junctions are prohibited
            if (it.isInLaneChange() || it.lane.road.junction != "-1") {
                return@forEach
            }

            val lanesToChange = it.lane.road.lanes.filter { newLane -> abs(newLane.laneId - it.lane.laneId) == 1}

            for (toLane in lanesToChange) {
                val balance = MOBIL.calcAccelerationBalance(it, toLane)
                if (balance > 0.0) {
                    println("Vehicle ${it.vehicleId} is lane changing ${balance}.")
                    it.pathBuilder.removePath(it)
                    it.performLaneChange(toLane)
                    return@forEach
                }
            }
        }
    }

    private val isPositionFree = object: WaypointSpawnAbilityChecker {
        override fun isFree(waypoint: Waypoint): Boolean {
            // TODO: Check vehicle length, other cars and so on...
            val lane = network.getLaneById(waypoint.roadId, waypoint.laneId)

            val minVeh = lane.getMinPositionVehicle()
            if (minVeh == null || minVeh!!.position > 50) {
                return true
            }
            return false
        }
    }

    private val createVehicle = object: VehicleCreationListener {
        override fun createVehicle(
            source: Waypoint,
            destination: Waypoint,
            onDespawn: RouteGeneratorDespawnListener
        ): Int? {
            // TODO: make additional checks, for example is source and destination can be connected
            val nw = Vehicle.NewVehicle(
                network,
                source,
                destination,
                onDespawn,
                rnd.nextInt(5, 9) * 4.0,
                rnd.nextDouble(1.5, 2.0))
            vehicles.add(nw)
            return nw.vehicleId
        }
    }

}
