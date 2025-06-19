package path

import SimulationConfig
import Waypoint
import network.Lane
import path.algorithms.IPathBuilder
import vehicle.Vehicle
import vehicle.VehicleDetector
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/*
    Some logical rules about mandatory lane changes:
        0) During dijkstra algorithm on one road we can have AT MOST 2 vertices
            (first is coming from another road, second (can be missing) mandatory lane change inside road.
            * Currently all the missing parts of path are filled during path recovery,
                but it's not necessary to do that.

        0.1) First road of path can contain MLC, but have to consider position of vehicle.
            For this each function accepts initPositionArgument, but uses it only when building new path.
        0.2) TODO: currently last roads on the path can not contain MLC (based on logic that buildings has one-lane roads)
            // TODO: it;s not right when part simulation!!
        1) When the next PathWaypoint is mandatory lane change - for nextVehicle scan we just need to check current road (even before mlcMaxDist).

        2) I can imagine scenarios where vehicle can use single Lane object twice, but it's very rare I think - will throw exception.
 */
class PathManager(private val algorithm: IPathBuilder) {

    private val vehiclesPaths = HashMap<Int, ArrayList<Path.PathWaypoint>>()

    fun isDestinationReachable(vehicle: Vehicle, initPosition: Double): Boolean {
        createPathIfNotExists(vehicle, initPosition)

        val path = vehiclesPaths.get(vehicle.vehicleId)
        if (path == null || path.isEmpty()) {
            return false
        }
        val despawnLane = path.last.lane
        return despawnLane.roadId.equals(vehicle.destination.roadId)
            && despawnLane.laneId.toString().equals(vehicle.destination.laneId)
    }

    /*
        Get distance to next MLC on path.
     */
    fun getNextMLCDistance(vehicle: Vehicle): Double {
        createPathIfNotExists(vehicle, vehicle.position)

        var curLane = vehicle.lane
        var first = true
        var acc_distance = 0.0
        var tmpAcc = curLane.length - vehicle.position

        // next path lanes
        while (acc_distance < SimulationConfig.MAX_VALUABLE_DISTANCE) {
            val nextLane = getNextPathLane(vehicle, curLane)
            if (nextLane == null) {
                return SimulationConfig.INF
            }
            if (nextLane.type == Path.PWType.MLC) {
                if (first) {
                    return nextLane.mlcMaxRoadOffset - vehicle.position
                }
                return acc_distance + nextLane.mlcMaxRoadOffset
            }
            first = false
            acc_distance += tmpAcc
            tmpAcc = nextLane.lane.length

            curLane = nextLane.lane
        }
        return SimulationConfig.INF
    }

    fun removePath(vehicle: Vehicle) {
        if (vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths.remove(vehicle.vehicleId)
        }
    }

    fun getNextPathLane(vehicle: Vehicle): Path.PathWaypoint? {
        createPathIfNotExists(vehicle, vehicle.position)

        return getNextPathLane(vehicle, vehicle.lane)
    }

    fun getNextPathLane(vehicle: Vehicle, lane: Lane): Path.PathWaypoint? {
        createPathIfNotExists(vehicle, vehicle.position)

        for(i in 0 until vehiclesPaths[vehicle.vehicleId]!!.size) {
            if (vehiclesPaths[vehicle.vehicleId]!!.get(i).lane == lane) {
                if (i < vehiclesPaths[vehicle.vehicleId]!!.size - 1) {
                    return vehiclesPaths[vehicle.vehicleId]?.get((i + 1))
                } else {
                    return null
                }
            }
        }

        return null
    }

    fun getNextRoads(vehicle: Vehicle): Sequence<VehicleDetector.VehicleLaneSequence> {
        createPathIfNotExists(vehicle, vehicle.position)

        fun generateNextRoads(vehicle: Vehicle, lane: Lane) : Sequence<VehicleDetector.VehicleLaneSequence> = sequence {
            var curLane = lane

            // initial lane
            var acc_distance = 0.0
            yield(VehicleDetector.VehicleLaneSequence(vehicle, lane, acc_distance, true))
            acc_distance += lane.road.troad.length - vehicle.position

            // next path lanes
            while (acc_distance < SimulationConfig.MAX_VALUABLE_DISTANCE) {
                val nextLane = getNextPathLane(vehicle, curLane)
                if (nextLane == null || nextLane.type == Path.PWType.MLC) {
                    break
                }

                yield(
                    VehicleDetector.VehicleLaneSequence(
                        vehicle,
                        nextLane.lane,
                        acc_distance,
                        false
                    )
                )
                acc_distance += nextLane.lane.length

                curLane = nextLane.lane
            }
        }

        return generateNextRoads(vehicle, vehicle.lane)
    }

    fun createPathIfNotExists(vehicle: Vehicle, initPosition: Double) {
        if (vehiclesPaths.containsKey(vehicle.vehicleId)) {
            return
        }
        vehiclesPaths[vehicle.vehicleId] = algorithm.getPath(
            vehicle.network,
            Waypoint(vehicle.lane.roadId, vehicle.lane.laneId.toString()),
            vehicle.destination,
            initPosition)

        val path = vehiclesPaths.get(vehicle.vehicleId)!!
        val usedLanes = HashSet<Lane>()
        path.forEach { usedLanes.add(it.lane) }

        if (usedLanes.size != path.size) {
            throw RuntimeException("Simulation bug, Vehicle path can use lane only single time.")
        }
    }
}
