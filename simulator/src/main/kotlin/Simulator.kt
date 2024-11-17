import network.Network
import opendrive.OpenDRIVE
import vehicle.Vehicle
import vehicle.model.MOBIL
import java.util.*

class Simulator(openDrive: OpenDRIVE) {

    val network: Network = Network(openDrive.road, openDrive.junction)
    val rnd= Random()
    var spawnTimer = 2.0

    fun update(dt: Double) {
        vehicles.forEach { it ->
            if (it.isInLaneChange()) {
                return@forEach
            }
            val idx = it.lane.road.lanes.indexOf(it.lane)
            if (idx - 1 >= 0) {
                val toLane = it.lane.road.lanes.get(idx - 1)
                val balance = MOBIL.calcAccelerationBalance(it, toLane)
                if (balance > 0.0) {
                    it.setNewLane(toLane)
                    return@forEach
                }
            }
            if (idx + 1 < it.lane.road.lanes.size) {
                val toLane = it.lane.road.lanes.get(idx + 1)
                val balance = MOBIL.calcAccelerationBalance(it, toLane)
                if (balance > 0.0) {
                    it.setNewLane(toLane)
                }
            }
        }

        vehicles.forEach { it ->
            it.update(dt)
        }

        spawnTimer += dt
        if (spawnTimer >= 0.5) {
            addVehicle()
            spawnTimer = 0.0
        }
    }

    val vehicles: ArrayList<Vehicle> = ArrayList()

    fun addVehicle() {

        val nw = Vehicle.NewVehicle(network.roads.get(0).lanes.get(rnd.nextInt(3)),
            rnd.nextInt(5, 9) * 4.0,
            rnd.nextDouble(1.5, 2.0))

        vehicles.add(nw)
    }

}
