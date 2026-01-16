package ru.nsu.trafficsimulator.backend.path.algorithms

import ru.nsu.trafficsimulator.backend.SimulationConfig
import ru.nsu.trafficsimulator.backend.Simulator
import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.Path
import ru.nsu.trafficsimulator.backend.path.algorithms.IPathBuilder.RoadWaypoint
import ru.nsu.trafficsimulator.backend.path.cost_function.ICostFunction
import ru.nsu.trafficsimulator.backend.utils.TimedCache
import java.util.*

class CachedDijkstraPathBuilder(
    private val network: Network,
    private val simulator: Simulator,
    private val costFunction: ICostFunction,
    private val cacheTimeout: Double = 20.0 * 5
) : IPathBuilder {

    private val dijkstraPathBuilder = DijkstraPathBuilder(network, costFunction)
    private val timedCache = TimedCache<Pair<Waypoint, Waypoint>, Pair<Double, List<Path.PathWaypoint>>>(cacheTimeout)

    override fun getPath(
        source: Waypoint,
        destination: Waypoint,
        initPosition: Double
    ): Pair<Double, List<Path.PathWaypoint>> {

        val srcLane = network.getLaneById(source.roadId, source.laneId.toInt())
        var cost = Double.MAX_VALUE
        var path: List<Path.PathWaypoint> = ArrayList<Path.PathWaypoint>()
        val curRoadWaypoint = RoadWaypoint(srcLane, Path.PWType.NORMAL)

        if (source == destination) {
            return Pair(curRoadWaypoint.lane.length, listOf(Path.PathWaypoint(
                Path.PWType.NORMAL,
                curRoadWaypoint.lane,
                -SimulationConfig.INF
            )))
        }

        // Option 1: Normal lane change
        // Explore the next lanes
        curRoadWaypoint.lane.getNextLane()?.forEach { toLane ->
            val toWaypoint = RoadWaypoint(toLane, Path.PWType.NORMAL)
            // We can not consider initPosition in cost, as first road will be fully traversed in every situation
            val (curPathCost, curPath) = timedCache.get(Pair(Waypoint(toWaypoint.lane.roadId, toWaypoint.lane.laneId.toString()), destination),
                simulator.currentTime,
                { key: Pair<Waypoint, Waypoint> -> dijkstraPathBuilder.getPath(key.first, key.second, 0.0) })
            val edgeWeight = costFunction.getLaneCost(curRoadWaypoint.lane, simulator.currentTime)
            if (edgeWeight + curPathCost < cost) {
                cost = edgeWeight + curPathCost

                // toMutableList() to make a copy
                val tmp = curPath.toMutableList()
                tmp.add(
                    0, Path.PathWaypoint(
                        Path.PWType.NORMAL,
                        curRoadWaypoint.lane,
                        -SimulationConfig.INF
                    )
                )
                path = tmp.toList()
            }
        }

        // Option 2: Single MLC and normal lane change after it
        curRoadWaypoint.lane.road.lanes.filter { toLane ->
            // Lanes that have the same direction...
            toLane.laneId * curRoadWaypoint.lane.laneId > 0
                // And not same lanes
                && toLane.laneId != curRoadWaypoint.lane.laneId
        }.forEach { toMLCLane ->
            val numLaneChanges = Math.abs(toMLCLane.laneId - curRoadWaypoint.lane.laneId)
            val availablePosition = toMLCLane.road.troad.length - initPosition
            val roadLanes =
                curRoadWaypoint.lane.road.lanes.filter { it.laneId * curRoadWaypoint.lane.laneId > 0 }.size

            if (!isMLCPossible(availablePosition, roadLanes, curRoadWaypoint.lane.laneId, toMLCLane.laneId))
                return@forEach

            val toWaypoint = RoadWaypoint(toMLCLane, Path.PWType.MLC)
            val MLCWeight = costFunction.getLaneChangeCost(numLaneChanges)

            // After that have to do normal LC
            toWaypoint.lane.getNextLane()?.forEach { toLane ->
                val toNormalWaypoint = RoadWaypoint(toLane, Path.PWType.NORMAL)
                // We can not consider initPosition in cost, as first road will be fully traversed in every situation
                val (curPathCost, curPath) = timedCache.get(Pair(Waypoint(toNormalWaypoint.lane.roadId, toNormalWaypoint.lane.laneId.toString()), destination),
                    simulator.currentTime,
                    { key: Pair<Waypoint, Waypoint> -> dijkstraPathBuilder.getPath(key.first, key.second, 0.0) })
                val edgeWeight = costFunction.getLaneCost(toWaypoint.lane, simulator.currentTime)
                if (MLCWeight + edgeWeight + curPathCost < cost) {
                    cost = MLCWeight + edgeWeight + curPathCost

                    // toMutableList() to make a copy
                    val tmp = curPath.toMutableList()
                    // Add MLC path
                    constructMLCReversedPathPart(curRoadWaypoint, toWaypoint).forEach { tmp.add(0, it) }
                    // Add starting point
                    tmp.add(
                        0, Path.PathWaypoint(
                            Path.PWType.NORMAL,
                            curRoadWaypoint.lane,
                            -SimulationConfig.INF
                        )
                    )
                    path = tmp.toList()
                }
            }
        }

        return Pair(cost, path)
    }
}
