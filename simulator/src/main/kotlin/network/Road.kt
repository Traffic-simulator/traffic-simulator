package network

import opendrive.TRoad
import opendrive.TRoadLinkPredecessorSuccessor
import kotlin.collections.ArrayList

class Road(val troad: TRoad) {

    val numLanes: Int
    val lanes: ArrayList<Lane> = ArrayList()
    val id: String = troad.id
    var predecessor: TRoadLinkPredecessorSuccessor? = troad.link.predecessor
    var successor: TRoadLinkPredecessorSuccessor? = troad.link.successor

    init {

        if (troad.lanes.laneSection[0].left != null) {
            lanes.addAll(troad.lanes.laneSection[0].left.lane.map{ it -> Lane(it, this, it.id.toInt())})
        }
        if (troad.lanes.laneSection[0].center != null) {
            lanes.addAll(troad.lanes.laneSection[0].center.lane.map{ it -> Lane(it, this, it.id.toInt())})
        }
        if (troad.lanes.laneSection[0].right != null) {
            lanes.addAll(troad.lanes.laneSection[0].right.lane.map{ it -> Lane(it, this, it.id.toInt())})
        }
        numLanes = lanes.size
    }



}