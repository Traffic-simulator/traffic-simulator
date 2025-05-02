package path_builder

import Waypoint
import network.Lane
import network.Network
import vehicle.Direction
import vehicle.Vehicle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random

// TODO: multiple lanes logic:
// Ask from pathBuilder is it ok to change the lane
// Consider lane changes as edges in graph
class ShortestPathBuilder: IPathBuilder {

    val vehiclesPaths = HashMap<Int, ArrayList<Pair<Lane, Boolean>>>()

    // In current usage will be called at first for every vehicle
    // !!! DON'T rely on that in prod realisation
    // Add some type of PathBuildingErrorException to log bad routes...
    override fun getNextPathLane(vehicle: Vehicle): Pair<Lane, Boolean>? {
        if (!vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths[vehicle.vehicleId] = getDijkstraShortestPath(vehicle.network, vehicle.source, vehicle.destination)
        }

        return getNextPathLane(vehicle, vehicle.lane, vehicle.direction)
    }

    fun getDijkstraShortestPath(network: Network, source: Waypoint, destination: Waypoint): ArrayList<Pair<Lane, Boolean>> {
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

    // In this realization direction is not used, but you have to use it in prod version
    override fun getNextPathLane(vehicle: Vehicle, lane: Lane, direction: Direction): Pair<Lane, Boolean>? {

        for(i in 0 until vehiclesPaths[vehicle.vehicleId]!!.size) {
            if (vehiclesPaths[vehicle.vehicleId]!!.get(i).first == lane) {
                if (i < vehiclesPaths[vehicle.vehicleId]!!.size - 1) {
                    return vehiclesPaths[vehicle.vehicleId]?.get((i + 1))
                } else {
                    return null;
                }
            }
        }

        // In case of errors return null
        return null
    }

    override fun getNextVehicle(vehicle: Vehicle, lane: Lane, direction: Direction, acc_distance: Double, initial_iteration: Boolean): Pair<Vehicle?, Double> {
        var closestVehicle: Vehicle? = null
        if (initial_iteration) {
            // TODO: use binary search and some kind of iterator or stream API
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
