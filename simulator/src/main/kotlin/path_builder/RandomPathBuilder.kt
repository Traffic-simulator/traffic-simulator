package path_builder

import network.Lane
import vehicle.Direction
import vehicle.Vehicle
import vehicle.Vehicle.ClosestJunction
import kotlin.random.Random


/**
 * When query for vehicle arrives for the first time, remember it and then use in next queries.
 * BE CAREFUL!!! CURRENT JUNCTION LOGIC DOESNT ALLOW PATH CHANGES.
 */
class RandomPathBuilder: IPathBuilder {

    val rnd = Random(12)

    val vehiclesPaths = HashMap<Int, ArrayList<Pair<Lane, Boolean>>>()


    // In current usage will be called at first for every vehicle
    // !!! DON'T rely on that in prod realisation
    override fun getNextPathLane(vehicle: Vehicle): Pair<Lane, Boolean>? {
        if (!vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths[vehicle.vehicleId] = ArrayList<Pair<Lane, Boolean>>()
            vehiclesPaths[vehicle.vehicleId]!!.add(Pair(vehicle.lane, false))

            var tmp_lane: Lane = vehicle.lane
            var tmp_dir: Direction = vehicle.direction

            // Init whole path right here.
            // Path until cycle or null
            do {
                val tmp = tmp_lane.getNextLane(tmp_dir)

                // Path until null
                if (tmp == null || tmp.size == 0) {
                    break
                }

                val nxtLaneIdx = rnd.nextInt(tmp.size)
                tmp_lane = tmp.get(nxtLaneIdx).first
                tmp_dir = if (tmp.get(nxtLaneIdx).second) tmp_dir.opposite(tmp_dir) else tmp_dir

                // Path until cycle
                if (!vehiclesPaths[vehicle.vehicleId]!!.filter({it.first == tmp_lane}).isEmpty()) {
                    break
                }

                vehiclesPaths[vehicle.vehicleId]!!.add(Pair(tmp_lane, tmp.get(nxtLaneIdx).second))
            } while(true);
        }

        return getNextPathLane(vehicle, vehicle.lane, vehicle.direction)
    }

    // In this realization direction is not used, but you have to use it in prod version
    override fun getNextPathLane(vehicle: Vehicle, lane: Lane, direction: Direction): Pair<Lane, Boolean>? {

        for(i in 0 until vehiclesPaths[vehicle.vehicleId]!!.size) {
            if (vehiclesPaths[vehicle.vehicleId]!!.get(i).first == lane) {
                if (i + 1 >= vehiclesPaths[vehicle.vehicleId]!!.size) return null
                return vehiclesPaths[vehicle.vehicleId]?.get(i + 1)
            }
        }

        // In case of errors return null
        return null
    }
}
