package network

import opendrive.TRoad

class Road(val troad: TRoad) {

    val numLanes: Int
    val lanes: List<Lane>

    init {
        lanes = troad.lanes.laneSection.get(0).left.lane.map{it -> Lane(it, this, it.id.toInt())}
        numLanes = lanes.size

    // Have to join later
    //lanes.addAll(troad.lanes.laneSection.get(0).right.lane.map{it -> Lane(it)})

    //troad.lanes.laneSection.get(0).right)
    }



}