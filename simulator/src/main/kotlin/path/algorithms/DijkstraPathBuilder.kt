package path.algorithms

import Waypoint
import network.Lane
import network.Network
import path.Path
import path.cost_function.ICostFunction
import java.util.*
import kotlin.collections.ArrayList

class DijkstraPathBuilder(private val costFunction: ICostFunction): IPathBuilder {

    override fun getPath(network: Network, source: Waypoint, destination: Waypoint, initPosition_: Double): ArrayList<Path.PathWaypoint> {
        var initPosition = initPosition_
        data class RoadWaypoint (
            val lane: Lane,
            val type: Path.PWType
        )

        val queue = PriorityQueue<Pair<RoadWaypoint, Double>>(compareBy { it.second })
        val cost = mutableMapOf<RoadWaypoint, Double>().withDefault { Double.MAX_VALUE }
        val par = mutableMapOf<RoadWaypoint, RoadWaypoint>()
        val srcLane = network.getLaneById(source.roadId, source.laneId)
        cost[RoadWaypoint(srcLane, Path.PWType.NORMAL)] = 0.0
        queue.add(RoadWaypoint(srcLane, Path.PWType.NORMAL) to 0.0)

        // States with type.MLC are the same, and imply that MLC can be done before the end of the road.
        //                               No matter from distance reserve.
        //                               For some sort of reality each MLC costs 10 meters of simple path.
        while (queue.isNotEmpty()) {
            val (curRoadWaypoint, curCost) = queue.poll()

            // Skip if we already found a better path
            if (curCost > cost.getValue(curRoadWaypoint)) continue

            fun updateDijkstraState(toWaypoint: RoadWaypoint, weight: Double) {
                val newDist = curCost + weight
                val oldDist = cost.getValue(toWaypoint)
                if (newDist < oldDist) {
                    cost.put(toWaypoint, newDist)
                    queue.add(toWaypoint to newDist)
                    par.put(toWaypoint, curRoadWaypoint)
                }
            }

            // Explore the next lanes
            curRoadWaypoint.lane.getNextLane()?.forEach {
                    toLane ->
                val toWaypoint = RoadWaypoint(toLane, Path.PWType.NORMAL)
                // We can not consider initPosition in cost, as first road will be fully traversed in every situation
                val weight = costFunction.getLaneCost(curRoadWaypoint.lane)
                updateDijkstraState(toWaypoint, weight)
            }

            // Explore MLC
            curRoadWaypoint.lane.road.lanes.filter { toLane ->
                // Lanes that have the same direction...
                toLane.laneId * curRoadWaypoint.lane.laneId > 0
                    // And not same lanes
                    && toLane.laneId != curRoadWaypoint.lane.laneId
            }.forEach {
                    toLane ->
                val numLaneChanges = Math.abs(toLane.laneId - curRoadWaypoint.lane.laneId)
                val availablePosition = toLane.road.troad.length - initPosition
                val roadLanes = curRoadWaypoint.lane.road.lanes.filter { it.laneId * curRoadWaypoint.lane.laneId > 0 }.size

                if (! isMLCPossible(availablePosition, roadLanes, curRoadWaypoint.lane.laneId, toLane.laneId))
                    return@forEach

                val toWaypoint = RoadWaypoint(toLane, Path.PWType.MLC)
                val weight = costFunction.getLaneChangeCost(numLaneChanges)
                updateDijkstraState(toWaypoint, weight)
            }

            // init position make sense only in first road.
            // Interesting detail is that MLC does not change initPosition and leave vehicle on the same road
            // but due to MLC non-zero cost there can't be done two MLC's inside one road - i.e. initPosition can be used only once
            initPosition = 0.0
        }

        // Path recovery
        val path = ArrayList<Path.PathWaypoint>()
        val dstLane = network.getLaneById(destination.roadId, destination.laneId)

        // TODO: Currently assume building can be reached without MLC on the last lane, it's not right during region simulation
        var curWaypoint = RoadWaypoint(dstLane, Path.PWType.NORMAL)
        if (!par.containsKey(curWaypoint)) {
            return path
        }
        while(par.containsKey(curWaypoint)) {
            // Two options:
            if (curWaypoint.type == Path.PWType.MLC) {
                // 1) Mandatory lane change, inside one road: have to add all lanes between with MLCMaxRoadOffset calculation
                assert(par.get(curWaypoint)!!.lane.road == curWaypoint.lane.road)

                val road = curWaypoint.lane.road
                val roadLanes = road.lanes.filter { it.laneId * curWaypoint.lane.laneId > 0 }.size
                val fromLaneId = par.get(curWaypoint)!!.lane.laneId
                var toLaneId = curWaypoint.lane.laneId


                toLaneId = toLaneId + (if (fromLaneId < toLaneId) -1 else 1)
                path.add(
                    Path.PathWaypoint(
                    Path.PWType.MLC,
                    curWaypoint.lane,
                    getMLCMaxRoadOffset(curWaypoint.lane.length, 1, roadLanes, toLaneId)))

                var i = 2
                while(toLaneId != fromLaneId) {
                    // On this step LC from tmpToLaneId to toLaneId
                    val tmpFromLaneId = toLaneId + (if (fromLaneId < toLaneId) -1 else 1)
                    path.add(
                        Path.PathWaypoint(
                        Path.PWType.MLC,
                        road.getLaneById(toLaneId),
                        getMLCMaxRoadOffset(curWaypoint.lane.length, i, roadLanes, tmpFromLaneId)))
                    toLaneId = tmpFromLaneId
                    i++
                }
            } else {
                // 2) Simple next road change
                path.add(
                    Path.PathWaypoint(
                    Path.PWType.NORMAL,
                    curWaypoint.lane,
                    -SimulationConfig.INF))
            }

            curWaypoint = par.get(curWaypoint)!!
        }
        path.add(
            Path.PathWaypoint(
            Path.PWType.NORMAL,
            curWaypoint.lane,
            -SimulationConfig.INF))

        Collections.reverse(path)
        return path
    }

    private fun isMLCPossible(length: Double, roadLanes: Int, fromLaneId_: Int, toLaneId: Int): Boolean {
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

    private fun getMLCMaxRoadOffset(roadLength: Double, numLC: Int, roadLanes: Int, fromLaneId: Int): Double {
        return roadLength - numLC * SimulationConfig.MLC_MIN_DISTANCE - 15.0 * (roadLanes - Math.abs(fromLaneId))
    }
}
