import junction_intersection.JunctionIntersectionFinder
import network.Network
import opendrive.OpenDRIVE
import route_generator.RouteGeneratorAPI
import vehicle.Direction
import vehicle.Vehicle
import vehicle.model.MOBIL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class Simulator(openDrive: OpenDRIVE, val spawnDetails: ArrayList<Waypoint>, val despawnDetails: ArrayList<Waypoint>, seed: Long) {

    val finder = JunctionIntersectionFinder(openDrive)
    val intersections = finder.findIntersection()
    val network: Network = Network(openDrive.road, openDrive.junction, intersections)
    val rnd = Random(seed)
    val routeGeneratorAPI = RouteGeneratorAPI(rnd, spawnDetails, despawnDetails)

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

    private val isPositionFree: (Waypoint) -> Boolean = {
        position ->
        // TODO: Check vehicle length, other cars and so on...
        val lane = network.getLaneById(position.roadId, position.laneId)

        val minVeh = lane.getMinPositionVehicle()
        if (minVeh == null) true
        else if (minVeh!!.position > 50) true
        else false
    }

    private val createVehicle: (Waypoint, Waypoint) -> Boolean = { source, destination ->
        val nw = Vehicle.NewVehicle(
            network,
            source,
            destination,
            rnd.nextInt(5, 9) * 4.0,
            rnd.nextDouble(1.5, 2.0))
        vehicles.add(nw)
        true
    }

}
