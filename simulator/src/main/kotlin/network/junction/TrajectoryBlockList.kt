package network.junction

import junction_intersection.Intersection
import opendrive.TJunctionConnection


// TODO: Currently blocking all trajectories except self. Waiting for geometry module

// TODO: Maybe use our Connection
// TODO: Add block reason
class TrajectoryBlockList(me: TJunctionConnection, others: List<TJunctionConnection>, val intersections: MutableList<Intersection>) {

    val blockList: List<TJunctionConnection>

    init {

        val filtered = intersections.filter { it.roadId1 == me.connectingRoad || it.roadId2 == me.connectingRoad }.toList()
        blockList = others.filter { otherConn ->
            filtered.find { (it.roadId1 == otherConn.connectingRoad || it.roadId2 == otherConn.connectingRoad) && otherConn.connectingRoad != me.connectingRoad} != null
        }.filter {it.incomingRoad != me.incomingRoad}
        if (blockList.find{it.id == me.id} != null) {
            println("wtf")
        }
//        intersections.filter {
//            it.roadId1 == me.connectingRoad || it.roadId2 == me.connectingRoad
//        }
        // blockList = others.filter { it.incomingRoad != me.incomingRoad }.toList()
    }

}
