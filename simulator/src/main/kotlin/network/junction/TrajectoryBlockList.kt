package network.junction

import opendrive.TJunctionConnection


// TODO: Currently blocking all trajectories except self. Waiting for geometry module

// TODO: Maybe use our Connection
// TODO: Add block reason
class TrajectoryBlockList(me: TJunctionConnection, others: List<TJunctionConnection>) {

    val blockList: List<TJunctionConnection>

    init {
        blockList = others.filter { it.incomingRoad != me.incomingRoad }.toList()
    }

}
