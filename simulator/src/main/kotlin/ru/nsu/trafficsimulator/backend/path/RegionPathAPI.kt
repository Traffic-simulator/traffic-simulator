package ru.nsu.trafficsimulator.backend.path

import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.algorithms.DijkstraPathBuilder
import ru.nsu.trafficsimulator.backend.path.cost_function.ICostFunction
import ru.nsu.trafficsimulator.backend.path.cost_function.StaticLengthCostFunction
import ru.nsu.trafficsimulator.backend.path.cost_function.StatsCostFunction

class RegionPathAPI(private val network: Network): IRegionPathAPI {

    val costFunction: ICostFunction = StatsCostFunction()
    val pathBuilder = DijkstraPathBuilder(network, costFunction)

    override fun isRegionRoad(roadId: Int, regionId: Int): Boolean {
        return isRegionRoad(roadId.toString(), regionId)
    }

    override fun isRegionRoad(roadId: String, regionId: Int): Boolean {
        return network.getRoadById(roadId).region == regionId
    }

    override fun getGlobalPath(source: Waypoint, destination: Waypoint): Pair<Double, List<Path.PathWaypoint>> {
        return pathBuilder.getPath(source, destination, 0.0)
    }

    override fun getPathTime(path: List<Path.PathWaypoint>, beforeExcluded: Path.PathWaypoint, time: Double): Double {
        val roads = path.takeWhile{ it != beforeExcluded }
        var cost = 0.0
        roads.forEach { cost += costFunction.getLaneCost(it.lane, time)}
        return cost
    }

    override fun getWaypointAvgSpeed(waypoint: Waypoint, time: Double): Double {
        return costFunction.getLaneAvgSpeed(network.getRoadById(waypoint.roadId).getLaneById(waypoint.laneId.toInt()), time)
    }
}
