package network

import opendrive.ELaneType
import opendrive.TRoad
import opendrive.TRoadLinkPredecessorSuccessor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Road(val troad: TRoad) {

    val numLanes: Int
    val lanes: ArrayList<Lane> = ArrayList()
    val id: String = troad.id
    var predecessor: TRoadLinkPredecessorSuccessor? = troad.link.predecessor
    var successor: TRoadLinkPredecessorSuccessor? = troad.link.successor

    init {

        if (troad.lanes.laneSection[0].left != null) {
            lanes.addAll(troad.lanes.laneSection[0].left.lane.filter { usingRoadTypes.contains(it.type) }.map{ it -> Lane(it, this, it.id.toInt())})
        }
        // TODO: Is it any cases when we need center lane?
//        if (troad.lanes.laneSection[0].center != null) {
//            lanes.addAll(troad.lanes.laneSection[0].center.lane.map{ it -> Lane(it, this, it.id.toInt())})
//        }
        if (troad.lanes.laneSection[0].right != null) {
            lanes.addAll(troad.lanes.laneSection[0].right.lane.filter { usingRoadTypes.contains(it.type) }.map{ it -> Lane(it, this, it.id.toInt())})
        }
        numLanes = lanes.size
    }

    companion object {
        val usingRoadTypes = HashSet<ELaneType> (Arrays.asList(
            ELaneType.DRIVING,
            ELaneType.EXIT,
            ELaneType.ENTRY,
            ELaneType.ON_RAMP,
            ELaneType.OFF_RAMP,
            ELaneType.CONNECTING_RAMP,
        ));
    }

}

