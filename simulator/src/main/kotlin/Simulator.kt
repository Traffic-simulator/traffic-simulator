import network.Network
import opendrive.OpenDRIVE
import vehicle.Vehicle
import vehicle.model.MOBIL
import java.util.*
import kotlin.math.abs

class Simulator(openDrive: OpenDRIVE, spawnDetails: SpawnDetails, seed: Long) {

    val network: Network = Network(openDrive.road, openDrive.junction)
    val rnd = Random(seed)
    var spawnTimer = 2.0

    // TODO: Correct lane id checking
    // TODO: staged updates
    fun update(dt: Double): ArrayList<Vehicle> {

        // Stage x: non mandatory lane changes
        vehicles.forEach { it ->
            if (it.isInLaneChange()) {
                return@forEach
            }

            val lanesToChange = it.lane.road.lanes.filter { newLane -> abs(newLane.laneId - it.lane.laneId) == 1}

            for (toLane in lanesToChange) {
                val balance = MOBIL.calcAccelerationBalance(it, toLane)
                if (balance > 0.0) {
                    it.setNewLane(toLane)
                    return@forEach
                }
            }
        }

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

    val vehicles: ArrayList<Vehicle> = ArrayList()

    fun addVehicle() {

        val side = rnd.nextInt(2)
        val nw: Vehicle
        if (side == 0) {
            nw = Vehicle.NewVehicle(network.roads.get(0).lanes.get(rnd.nextInt(2)),
                rnd.nextInt(5, 9) * 4.0,
                rnd.nextDouble(1.5, 2.0))

        } else {
            nw = Vehicle.NewVehicle(network.roads.get(2).lanes.get(rnd.nextInt(2, 4)),
                rnd.nextInt(5, 9) * 4.0,
                rnd.nextDouble(1.5, 2.0))
        }

        vehicles.add(nw)
    }

}
