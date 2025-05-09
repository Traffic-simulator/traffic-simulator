package network.junction

import junction_intersection.Intersection
import opendrive.TJunctionConnection

class TrajectoryBlockList(me: TJunctionConnection, others: List<TJunctionConnection>, val intersections: MutableList<Intersection>) {

    val blockList: List<TJunctionConnection>

    init {
        val filtered = intersections.filter { it.roadId1 == me.connectingRoad || it.roadId2 == me.connectingRoad }.toList()
        blockList = others.filter { otherConn ->
            filtered.find { (it.roadId1 == otherConn.connectingRoad || it.roadId2 == otherConn.connectingRoad) && otherConn.connectingRoad != me.connectingRoad} != null
        }.filter {it.incomingRoad != me.incomingRoad}
        // TODO: also consider outcoming roads!
    }
}
