package ru.nsu.trafficsimulator.backend.path.algorithms

import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.Path
import ru.nsu.trafficsimulator.backend.path.cost_function.ICostFunction
import ru.nsu.trafficsimulator.backend.SimulationConfig
import ru.nsu.trafficsimulator.backend.path.algorithms.IPathBuilder.RoadWaypoint
import java.util.*
import kotlin.collections.ArrayList

class DijkstraPathBuilder(private val network: Network, private val costFunction: ICostFunction): IPathBuilder {

    // Source always NORMAL
    // Destination NORMAL or MLC
    override fun getPath(source: Waypoint, destination: Waypoint, initPosition_: Double): Pair<Double, List<Path.PathWaypoint>> {
        var initPosition = initPosition_

        val srcLane = network.getLaneById(source.roadId, source.laneId.toInt())
        val curRoadWaypoint = RoadWaypoint(srcLane, Path.PWType.NORMAL)
        if (source == destination) {
            return Pair(curRoadWaypoint.lane.length, listOf(Path.PathWaypoint(
                Path.PWType.NORMAL,
                curRoadWaypoint.lane,
                -SimulationConfig.INF
            )))
        }

        val queue = PriorityQueue<Pair<RoadWaypoint, Double>>(compareBy { it.second })
        val cost = mutableMapOf<RoadWaypoint, Double>().withDefault { Double.MAX_VALUE }
        val par = mutableMapOf<RoadWaypoint, RoadWaypoint>()
        cost[RoadWaypoint(srcLane, Path.PWType.NORMAL)] = 0.0
        queue.add(RoadWaypoint(srcLane, Path.PWType.NORMAL) to 0.0)

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
                val weight = costFunction.getLaneCost(curRoadWaypoint.lane, 0.0)
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

        // Let's choose best option between NORMAL and MLC
        val dstWaypoint1 = RoadWaypoint(dstLane, Path.PWType.NORMAL)
        val dstWaypoint2 = RoadWaypoint(dstLane, Path.PWType.MLC)
        if (!cost.containsKey(dstWaypoint1) && !cost.containsKey(dstWaypoint2)) {
            return Pair(SimulationConfig.INF, path)
        }
        var curWaypoint = when {
            !cost.containsKey(dstWaypoint1) -> dstWaypoint2
            !cost.containsKey(dstWaypoint2) -> dstWaypoint1
            cost[dstWaypoint1]!! < cost[dstWaypoint2]!! -> dstWaypoint1
            else -> dstWaypoint2
        }

        val overallCost = cost[curWaypoint]!!
        while(par.containsKey(curWaypoint)) {
            // Two options:
            if (curWaypoint.type == Path.PWType.MLC) {
                path.addAll(constructMLCReversedPathPart(par.get(curWaypoint)!!, curWaypoint))
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
        return Pair(overallCost, path.toList())
    }


}
