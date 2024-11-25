import network.Network
import opendrive.OpenDRIVE
import vehicle.Direction
import vehicle.Vehicle
import vehicle.model.MOBIL
import java.util.*
import kotlin.math.abs

class Simulator(openDrive: OpenDRIVE, val spawnDetails: SpawnDetails, seed: Long) {

    val network: Network = Network(openDrive.road, openDrive.junction)
    val rnd = Random(seed)

    val vehicles: ArrayList<Vehicle> = ArrayList()
    var spawnTimer = 2.0

    // TODO: Correct lane id checking
    // TODO: staged updates
    fun update(dt: Double): ArrayList<Vehicle> {

        processNonmandatoryLaneChanges()

        // Stage y:
        vehicles.forEach { it ->
            it.update(dt)
        }

        // despawn vehicles
        vehicles.removeAll { it.despawned == true}

        spawnTimer += dt
        if (spawnTimer >= 0.5) {
            addVehicle()
            spawnTimer = 0.0
        }

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
                    it.performLaneChange(toLane)
                    return@forEach
                }
            }
        }
    }

    fun isPositionFree(position: Triple<String, String, Direction>): Boolean {
        // TODO: Check vehicle length, other cars and so on...
        val lane = network.getLaneById(position.first, position.second)

        val minVeh = lane.getMinPositionVehicle()
        if (minVeh == null) {
            return true
        }

        if (minVeh.position > 10) {
            return true
        }

        return false
    }

    fun addVehicle() {
        val availablePositions = spawnDetails.spawnPair.filter { isPositionFree(it) }
        val idx = rnd.nextInt(availablePositions.size)

        val nw = Vehicle.NewVehicle(
            network,
            network.getLaneById(availablePositions[idx].first, availablePositions[idx].second),
            availablePositions[idx].third,
            rnd.nextInt(5, 9) * 4.0,
            rnd.nextDouble(1.5, 2.0))
        vehicles.add(nw)
    }

}
