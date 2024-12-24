package path_builder

import SimulationConfig
import Simulator
import network.Lane
import vehicle.Direction
import vehicle.Vehicle
import vehicle.Vehicle.ClosestJunction
import kotlin.random.Random


/**
 * When query for vehicle arrives for the first time, remember it and then use in next queries.
 * BE CAREFUL!!! CURRENT JUNCTION LOGIC DOESNT ALLOW PATH CHANGES.
 */
class RandomPathBuilder(seed: Long): IPathBuilder {

    val rnd = Random(seed)

    val vehiclesPaths = HashMap<Int, ArrayList<Pair<Lane, Boolean>>>()
    val cycleInfo = HashMap<Int, Pair<Lane, Boolean>>()

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
                    cycleInfo.put(vehicle.vehicleId, Pair(tmp_lane, tmp.get(nxtLaneIdx).second))
                    break
                }

                vehiclesPaths[vehicle.vehicleId]!!.add(Pair(tmp_lane, tmp.get(nxtLaneIdx).second))
            } while(true);
        }

        return getNextPathLane(vehicle, vehicle.lane, vehicle.direction)
    }

    override fun removePath(vehicle: Vehicle) {
        if (vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths.remove(vehicle.vehicleId)
        }
    }

    // In this realization direction is not used, but you have to use it in prod version
    override fun getNextPathLane(vehicle: Vehicle, lane: Lane, direction: Direction): Pair<Lane, Boolean>? {

        for(i in 0 until vehiclesPaths[vehicle.vehicleId]!!.size) {
            if (vehiclesPaths[vehicle.vehicleId]!!.get(i).first == lane) {
                if (i < vehiclesPaths[vehicle.vehicleId]!!.size - 1) {
                    return vehiclesPaths[vehicle.vehicleId]?.get((i + 1))
                }

                if (cycleInfo.containsKey(vehicle.vehicleId)) {
                    return cycleInfo[vehicle.vehicleId]
                } else {
                    return null
                }
            }
        }

        // In case of errors return null
        return null
    }

    override fun getNextVehicle(vehicle: Vehicle, lane: Lane, direction: Direction, acc_distance: Double, initial_iteration: Boolean): Pair<Vehicle?, Double> {
        var closestVehicle: Vehicle? = null
        if (initial_iteration) {
            // TODO: use binary search
            lane.vehicles.forEach{ it ->
                if (it.position > vehicle.position + SimulationConfig.EPS) {
                    if (closestVehicle == null) {
                        closestVehicle = it
                    } else {
                        if (closestVehicle!!.position > it.position) {
                            closestVehicle = it
                        }
                    }
                }
            }
            if (closestVehicle != null) {
                val distance = closestVehicle!!.position - closestVehicle!!.length - vehicle.position
                if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                    return Pair(null, SimulationConfig.INF)
                }
                return Pair(closestVehicle, distance)
            }
        } else {
            closestVehicle = lane.getMinPositionVehicle()
        }

        if (acc_distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
            return Pair(null, SimulationConfig.INF)
        }

        if (closestVehicle == null) {
            val nextLane = getNextPathLane(vehicle, lane, direction)
            if (nextLane == null) {
                return Pair(null, SimulationConfig.INF)
            }

            return getNextVehicle(vehicle, nextLane.first, vehicle.direction.opposite(nextLane.second), acc_distance + lane.road.troad.length, false)
        } else {
            val distance = closestVehicle!!.position - closestVehicle!!.length - vehicle.position + acc_distance
            if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }
            return Pair(closestVehicle, distance)
        }
    }


}
