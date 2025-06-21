package ru.nsu.trafficsimulator.backend.path.algorithms

import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.Path

interface IPathBuilder {

    fun getPath(
        network: Network,
        source: Waypoint,
        destination: Waypoint,
        initPosition_: Double
    ): ArrayList<Path.PathWaypoint>

}
