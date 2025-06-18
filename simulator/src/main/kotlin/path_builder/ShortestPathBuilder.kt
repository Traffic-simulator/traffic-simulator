package path_builder

import SimulationConfig
import Waypoint
import jakarta.validation.Path
import network.Lane
import network.Network
import vehicle.Direction
import vehicle.Vehicle
import vehicle.VehicleDetector
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.random.Random


// TODO: Use proxy or dectorator pattern
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

        2) TODO: I can imagine scenarios where vehicle can use single Lane object twice, but it's very rare I think.
 */
// TODO: because of MOBIL logic we will rebuild path every nmlc/mlc try, maybe somehow optimize it?
class ShortestPathBuilder: IPathBuilder {

    val vehiclesPaths = HashMap<Int, ArrayList<IPathBuilder.PathWaypoint>>()

    override fun isDestinationReachable(vehicle: Vehicle, initPosition: Double): Boolean {
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
    override fun getNextMLCDistance(vehicle: Vehicle): Double {
        createPathIfNotExists(vehicle, vehicle.position)

        var curDir = vehicle.direction
        var first = true
        var acc_distance = 0.0
        var curLane = vehicle.lane
        var tmpAcc = curLane.length - vehicle.position

        val path = vehiclesPaths[vehicle.vehicleId]!!
        var curWaypointIndex = path.indexOfFirst { it.lane == curLane }
        if (curWaypointIndex == -1) {
            return SimulationConfig.INF
        }

        // next path lanes
        while (acc_distance < SimulationConfig.MAX_VALUABLE_DISTANCE && curWaypointIndex < path.size - 1) {
            curWaypointIndex += 1
            val nextLane = path[curWaypointIndex]
            if (nextLane.type == IPathBuilder.PWType.MLC) {
                if (first) {
                    return nextLane.mlcMaxRoadOffset - vehicle.position
                }
                return acc_distance + nextLane.mlcMaxRoadOffset
            }
            first = false
            acc_distance += tmpAcc
            tmpAcc = nextLane.lane.length

            curLane = nextLane.lane
            curDir = curDir.opposite(nextLane.isDirectionOpposite)
        }
        return SimulationConfig.INF
    }

    private fun getDijkstraShortestPath(network: Network, source: Waypoint, destination: Waypoint, initPosition: Double): ArrayList<IPathBuilder.PathWaypoint> {
        data class RoadWaypoint (
            val lane: Lane,
            val direction: Direction,
            val type: IPathBuilder.PWType
        )

        val queue = PriorityQueue<Pair<RoadWaypoint, Double>>(compareBy { it.second })
        val dist = mutableMapOf<RoadWaypoint, Double>().withDefault { Double.MAX_VALUE }
        val par = mutableMapOf<RoadWaypoint, RoadWaypoint>()
        var first = true
        val srcLane = network.getLaneById(source.roadId, source.laneId)
        dist[RoadWaypoint(srcLane, source.direction, IPathBuilder.PWType.NORMAL)] = 0.0
        queue.add(RoadWaypoint(srcLane, source.direction, IPathBuilder.PWType.NORMAL) to 0.0)

        // States with type.MLC are the same, and imply that MLC can be done before the end of the road.
        //                               No matter from distance reserve.
        //                               For some sort of reality each MLC costs 10 meters of simple path.
        while (queue.isNotEmpty()) {
            val (curRoadWaypoint, curDist) = queue.poll()

            // Skip if we already found a better path
            if (curDist > dist.getValue(curRoadWaypoint)) continue

            // Explore the next lanes
            curRoadWaypoint.lane.getNextLane(curRoadWaypoint.direction)?.forEach {
                (toLane, dirChange) ->
                    val toWaypoint = RoadWaypoint(toLane, curRoadWaypoint.direction.opposite(dirChange), IPathBuilder.PWType.NORMAL)
                    val newDist = curDist + curRoadWaypoint.lane.road.troad.length - (if (first) initPosition else 0.0) // TODO: Think about it
                    val oldDist = dist.getValue(toWaypoint)
                    if (newDist < oldDist) {
                        dist.put(toWaypoint, newDist)
                        queue.add(toWaypoint to newDist)
                        par.put(toWaypoint, curRoadWaypoint)
                    }
            }

            // Explore MLC.
            curRoadWaypoint.lane.road.lanes.filter { toLane ->
                // Lanes that have the same direction...
                toLane.laneId * curRoadWaypoint.lane.laneId > 0
                // And not same lanes
                    && toLane.laneId != curRoadWaypoint.lane.laneId
            }.forEach {
                toLane ->
                    val numLaneChanges = Math.abs(toLane.laneId - curRoadWaypoint.lane.laneId)
                    val availablePosition = toLane.road.troad.length - (if (first) initPosition else 0.0)
                    val roadLanes = curRoadWaypoint.lane.road.lanes.filter { it.laneId * curRoadWaypoint.lane.laneId > 0 }.size

                    if (! isMLCPossible(availablePosition, roadLanes, curRoadWaypoint.lane.laneId, toLane.laneId))
                        return@forEach

                    val toWaypoint = RoadWaypoint(toLane, curRoadWaypoint.direction, IPathBuilder.PWType.MLC)

                    // Add some distance to do not create cycles with mandatory lane changes
                    val newDist = curDist + 10.0 * numLaneChanges
                    val oldDist = dist.getValue(toWaypoint)
                    if (newDist < oldDist) {
                        dist.put(toWaypoint, newDist)
                        queue.add(toWaypoint to newDist)
                        par.put(toWaypoint, curRoadWaypoint)
                    }
            }
            first = false
        }

        // Path recovery
        val path = ArrayList<IPathBuilder.PathWaypoint>()
        val dstLane = network.getLaneById(destination.roadId, destination.laneId)

        // TODO: Currently assume building can be reached without MLC on the last lane
        var curWaypoint = RoadWaypoint(dstLane, destination.direction, IPathBuilder.PWType.NORMAL)
        if (!par.containsKey(curWaypoint)) {
            return path
        }
        while(par.containsKey(curWaypoint)) {
            // Two options:
            if (curWaypoint.type == IPathBuilder.PWType.MLC) {
                // 1) Mandatory lane change, inside one road: have to add all lanes between with MLCMaxRoadOffset calculation
                assert(par.get(curWaypoint)!!.lane.road == curWaypoint.lane.road)

                val road = curWaypoint.lane.road
                val roadLanes = road.lanes.filter { it.laneId * curWaypoint.lane.laneId > 0 }.size
                val fromLaneId = par.get(curWaypoint)!!.lane.laneId
                var toLaneId = curWaypoint.lane.laneId


                toLaneId = toLaneId + (if (fromLaneId < toLaneId) -1 else 1)
                path.add(IPathBuilder.PathWaypoint(
                    IPathBuilder.PWType.MLC,
                    curWaypoint.lane,
                    false,
                    getMLCMaxRoadOffset(curWaypoint.lane.length, 1, roadLanes, toLaneId))) // to be find path using available space. TODO: another logic!

                var i = 2
                while(toLaneId != fromLaneId) {
                    // On this step LC from tmpToLaneId to toLaneId
                    val tmpFromLaneId = toLaneId + (if (fromLaneId < toLaneId) -1 else 1)
                    path.add(IPathBuilder.PathWaypoint(
                        IPathBuilder.PWType.MLC,
                        road.getLaneById(toLaneId),
                        false,
                        getMLCMaxRoadOffset(curWaypoint.lane.length, i, roadLanes, tmpFromLaneId)))
                    toLaneId = tmpFromLaneId
                    i++
                }
            } else {
                // 2) Simple next road change
                val oldDir = par.get(curWaypoint)!!.direction
                val curDir = curWaypoint.direction
                path.add(IPathBuilder.PathWaypoint(
                    IPathBuilder.PWType.NORMAL,
                    curWaypoint.lane,
                    oldDir != curDir,
                    -SimulationConfig.INF))
            }

            curWaypoint = par.get(curWaypoint)!!
        }
        path.add(IPathBuilder.PathWaypoint(
            IPathBuilder.PWType.NORMAL,
            curWaypoint.lane,
            false,
            -SimulationConfig.INF))

        Collections.reverse(path)
        return path
    }

    fun isMLCPossible(length: Double, roadLanes: Int, fromLaneId_: Int, toLaneId: Int): Boolean {
        // Same logic as in path recovery, but here from and to are correct.
        var numLC = Math.abs(fromLaneId_ - toLaneId)

        if (getMLCMaxRoadOffset(length, numLC, roadLanes, fromLaneId_) < 0) {
            return false
        }
        var fromLaneId = fromLaneId_ + (if (toLaneId < fromLaneId_) -1 else 1)
        numLC--

        while(fromLaneId != toLaneId) {
            if (getMLCMaxRoadOffset(length, numLC, roadLanes, fromLaneId) < 0) {
                return false
            }

            fromLaneId = fromLaneId + (if (toLaneId < fromLaneId) -1 else 1)
            numLC--
        }
        return true
    }

    fun getMLCMaxRoadOffset(roadLength: Double, numLC: Int, roadLanes: Int, fromLaneId: Int): Double {
        return roadLength - numLC * SimulationConfig.MLC_MIN_DISTANCE - 15.0 * (roadLanes - Math.abs(fromLaneId))
    }

    override fun removePath(vehicle: Vehicle) {
        if (vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths.remove(vehicle.vehicleId)
        }
    }

    // TODO: Do not think that vehicle is in the beggining of the road
    override fun getNextPathLane(vehicle: Vehicle): IPathBuilder.PathWaypoint? {
        return getNextPathLane(vehicle, vehicle.lane)
    }

    override fun getNextPathLane(vehicle: Vehicle, lane: Lane): IPathBuilder.PathWaypoint? {
        createPathIfNotExists(vehicle, vehicle.position)

        for(i in 0 until vehiclesPaths[vehicle.vehicleId]!!.size) {
            if (vehiclesPaths[vehicle.vehicleId]!![i].lane == lane) {
                if (i < vehiclesPaths[vehicle.vehicleId]!!.size - 1) {
                    return vehiclesPaths[vehicle.vehicleId]?.get((i + 1))
                } else {
                    return null
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
            val nextLane = getNextPathLane(vehicle, curLane)
            if (nextLane == null || nextLane.type == IPathBuilder.PWType.MLC) {
                break
            }

            yield(
                VehicleDetector.VehicleLaneSequence(
                    vehicle,
                    nextLane.lane,
                    curDir.opposite(nextLane.isDirectionOpposite),
                    acc_distance,
                    false
                )
            )
            acc_distance += nextLane.lane.length

            curLane = nextLane.lane
            curDir = curDir.opposite(nextLane.isDirectionOpposite)
        }
    }

    override fun getNextVehicle(vehicle: Vehicle): Pair<Vehicle?, Double> {
        createPathIfNotExists(vehicle, vehicle.position)

        return VehicleDetector.getNextVehicle(vehicle, generateNextRoads(vehicle, vehicle.lane, vehicle.direction))
    }

    private fun createPathIfNotExists(vehicle: Vehicle, initPosition: Double) {
        if (!vehiclesPaths.containsKey(vehicle.vehicleId)) {
            vehiclesPaths[vehicle.vehicleId] = getDijkstraShortestPath(
                vehicle.network,
                Waypoint(vehicle.lane.roadId, vehicle.lane.laneId.toString(), vehicle.direction),
                vehicle.destination,
                initPosition)
        }

        // IF DEBUG...
        // Prove that all lines are distinct
        val path = vehiclesPaths.get(vehicle.vehicleId)!!

        val usedLanes = HashSet<Lane>()
        path.forEach { usedLanes.add(it.lane) }

        if (usedLanes.size != path.size) {
            assert(usedLanes.size == path.size, { "Vehicle path can use lane only single time" })
        }
    }
}
