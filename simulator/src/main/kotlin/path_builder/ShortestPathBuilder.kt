package path_builder

import Waypoint
import network.Lane
import network.Network
import vehicle.Direction
import vehicle.Vehicle
import vehicle.VehicleDetector
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random

class ShortestPathBuilder: IPathBuilder {

    val vehiclesPaths = HashMap<Int, ArrayList<Pair<Lane, Boolean>>>()

    // Add some type of PathBuildingErrorException to log bad routes...
    override fun getNextPathLane(vehicle: Vehicle): Pair<Lane, Boolean>? {
        if (!vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths[vehicle.vehicleId] = getDijkstraShortestPath(
                vehicle.network,
                Waypoint(vehicle.lane.roadId, vehicle.lane.laneId.toString(), vehicle.direction),
                vehicle.destination)
        }

        return getNextPathLane(vehicle, vehicle.lane, vehicle.direction)
    }

    private fun getDijkstraShortestPath(network: Network, source: Waypoint, destination: Waypoint): ArrayList<Pair<Lane, Boolean>> {
        val queue = PriorityQueue<Pair<RoadWaypoint, Double>>(compareBy { it.second })
        val dist = mutableMapOf<RoadWaypoint, Double>().withDefault { Double.MAX_VALUE }
        val par = mutableMapOf<RoadWaypoint, RoadWaypoint>()

        val srcLane = network.getLaneById(source.roadId, source.laneId)
        dist[RoadWaypoint(srcLane, source.direction)] = 0.0
        queue.add(RoadWaypoint(srcLane, source.direction) to 0.0)

        while (queue.isNotEmpty()) {
            val (curRoadWaypoint, curDist) = queue.poll()

            // Skip if we already found a better path
            if (curDist > dist.getValue(curRoadWaypoint)) continue

            // Explore all neighbors
            curRoadWaypoint.lane.getNextLane(curRoadWaypoint.direction)?.forEach {
                (toLane, dirChange) ->
                    val toWaypoint = RoadWaypoint(toLane, curRoadWaypoint.direction.opposite(dirChange))
                    val newDist = curDist + curRoadWaypoint.lane.road.troad.length
                    val oldDist = dist.getValue(toWaypoint)
                    if (newDist < oldDist) {
                        dist.put(toWaypoint, newDist)
                        queue.add(toWaypoint to newDist)
                        par.put(toWaypoint, curRoadWaypoint)
                    }
            }
        }

        // Get path
        val path = ArrayList<Pair<Lane, Boolean>>()
        val dstLane = network.getLaneById(destination.roadId, destination.laneId)
        var curWaypoint = RoadWaypoint(dstLane, destination.direction)
        while(par.containsKey(curWaypoint)) {
            val oldDir = par.get(curWaypoint)!!.direction
            val curDir = curWaypoint.direction
            path.add(Pair(curWaypoint.lane, oldDir != curDir))
            curWaypoint = par.get(curWaypoint)!!
        }
        path.add(Pair(curWaypoint.lane, false))

        Collections.reverse(path)

        return path
    }

    data class RoadWaypoint (
        val lane: Lane,
        val direction: Direction
    )

    override fun removePath(vehicle: Vehicle) {
        if (vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths.remove(vehicle.vehicleId)
        }
    }

    override fun getNextPathLane(vehicle: Vehicle, lane: Lane, direction: Direction): Pair<Lane, Boolean>? {
        if (!vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths[vehicle.vehicleId] = getDijkstraShortestPath(
                vehicle.network,
                Waypoint(vehicle.lane.roadId, vehicle.lane.laneId.toString(), vehicle.direction),
                vehicle.destination)
        }

        for(i in 0 until vehiclesPaths[vehicle.vehicleId]!!.size) {
            if (vehiclesPaths[vehicle.vehicleId]!!.get(i).first == lane) {
                if (i < vehiclesPaths[vehicle.vehicleId]!!.size - 1) {
                    return vehiclesPaths[vehicle.vehicleId]?.get((i + 1))
                } else {
                    return null;
                }
            }
        }

        return null
    }

    private fun generateNextRoads(vehicle: Vehicle, lane: Lane, direction: Direction) : Sequence<VehicleDetector.VehicleLaneSequence> = sequence {
        var curLane = lane
        var curDir = direction

        // initial lane
        var acc_distance = 0.0
        yield(VehicleDetector.VehicleLaneSequence(vehicle, lane, direction, acc_distance, true))
        acc_distance += lane.road.troad.length - vehicle.position

        // next path lanes
        while (acc_distance < SimulationConfig.MAX_VALUABLE_DISTANCE) {
            val nextLane = getNextPathLane(vehicle, curLane, curDir)
            if (nextLane == null) {
                break
            }

            yield(
                VehicleDetector.VehicleLaneSequence(
                    vehicle,
                    nextLane.first,
                    curDir.opposite(nextLane.second),
                    acc_distance,
                    false
                )
            )
            acc_distance += nextLane.first.road.troad.length

            curLane = nextLane.first
            curDir = curDir.opposite(nextLane.second)
        }

    }

    override fun getNextVehicle(vehicle: Vehicle, lane: Lane, direction: Direction): Pair<Vehicle?, Double> {
        if (!vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths[vehicle.vehicleId] = getDijkstraShortestPath(
                vehicle.network,
                Waypoint(vehicle.lane.roadId, vehicle.lane.laneId.toString(), vehicle.direction),
                vehicle.destination)
        }

        return VehicleDetector.getNextVehicle(vehicle, generateNextRoads(vehicle, lane, direction))
    }

}
