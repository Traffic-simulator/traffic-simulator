package path.algorithms

import Waypoint
import network.Network
import path.Path

interface IPathBuilder {

    fun getPath(
        network: Network,
        source: Waypoint,
        destination: Waypoint,
        initPosition_: Double
    ): ArrayList<Path.PathWaypoint>

}
