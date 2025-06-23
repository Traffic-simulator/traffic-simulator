package ru.nsu.trafficsimulator.backend.path

import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.algorithms.DijkstraPathBuilder
import ru.nsu.trafficsimulator.backend.path.cost_function.ICostFunction
import ru.nsu.trafficsimulator.backend.path.cost_function.StaticLengthCostFunction

class RegionPathAPI(private val network: Network): IRegionPathAPI {

    // TODO: Use CachedDijkstraPathBuilder
    val costFunction: ICostFunction = StaticLengthCostFunction()
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

    override fun getPathTime(path: List<Path.PathWaypoint>, beforeExcluded: Path.PathWaypoint): Double {
        val roads = path.takeWhile{ it != beforeExcluded }
        var cost = 0.0
        // TODO: USE TIMED COST FUNCTION
        roads.forEach { cost += costFunction.getLaneCost(it.lane) / 16.0 /* TODO: drop this divison later*/ }
        return cost
    }

    override fun getWaypointAvgSpeed(waypoint: Waypoint): Double {
        return 8.0 // for now it's ok
    }
}
