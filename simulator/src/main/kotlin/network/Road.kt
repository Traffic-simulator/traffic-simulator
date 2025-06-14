package network

import network.signals.Signal
import opendrive.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Road(val troad: TRoad) {

    val numLanes: Int
    var lanes: ArrayList<Lane> = ArrayList()
    val id: String = troad.id
    val junction: String = troad.junction
    var predecessor: TRoadLinkPredecessorSuccessor? = troad.link.predecessor
    var successor: TRoadLinkPredecessorSuccessor? = troad.link.successor
    var signals: TRoadSignals? = troad.signals

    init {

        if (troad.lanes.laneSection[0].left != null) {
            lanes.addAll(troad.lanes.laneSection[0].left.lane.filter { /* usingRoadTypes.contains(it.type) */ true }.map{ it -> Lane(it, this, it.id.toInt())})
        }
        // TODO: Is it any cases when we need center lane?
//        if (troad.lanes.laneSection[0].center != null) {
//            lanes.addAll(troad.lanes.laneSection[0].center.lane.map{ it -> Lane(it, this, it.id.toInt())})
//        }
        if (troad.lanes.laneSection[0].right != null) {
            lanes.addAll(troad.lanes.laneSection[0].right.lane.filter { /* usingRoadTypes.contains(it.type) */ true  }.map{ it -> Lane(it, this, it.id.toInt())})
        }

        val laneSectionSize = troad.lanes.laneSection.size
        if (laneSectionSize > 1) {
            val lastLaneSection = troad.lanes.laneSection[laneSectionSize - 1]
            // left
            if (lastLaneSection.left != null) {
                for (lane in lastLaneSection.left.lane.filter { /*usingRoadTypes.contains(it.type)*/ true }) {
                    val laneid = lane.id
                    val laneSucc = lane.link.successor
                    val targetLaneIndex= lanes.indexOfFirst { it.laneId.toBigInteger() == laneid }
                    if (!lanes[targetLaneIndex].laneLink?.successor?.equals(laneSucc)!!) {
                        println("Changed successor for road ${troad.id} and lane ${lanes[targetLaneIndex].laneId}:" +
                            " ${lanes[targetLaneIndex].laneLink?.successor?.first()?.id} -> ${laneSucc.first().id}")
                        lanes[targetLaneIndex].laneLink?.successor?.first()?.id = laneSucc.first().id
                    }
                }
            }

            // right
            if (lastLaneSection.right != null) {
                for (lane in lastLaneSection.right.lane.filter { /* usingRoadTypes.contains(it.type) */ true }) {
                    var laneid = lane.id
                    var laneSucc = lane.link.successor
                    var targetLaneIndex= lanes.indexOfFirst { it.laneId.toBigInteger() == laneid }
                    if (!lanes[targetLaneIndex].laneLink?.successor?.equals(laneSucc)!!) {
                        println("Changed successor for road ${troad.id} and lane ${lanes[targetLaneIndex].laneId}:" +
                            " ${lanes[targetLaneIndex].laneLink?.successor?.first()?.id} -> ${laneSucc.first().id}")
                        lanes[targetLaneIndex].laneLink?.successor?.first()?.id = laneSucc.first().id
                    }
                }
            }
        }
        numLanes = lanes.size

        if (signals != null) {
            for (signal in signals!!.signal.filter { it.dynamic == TYesNo.YES && Regex("[0-9]*-[0-9]*-[0-9]*").matches(it.subtype) }) {
                // привязываем к тем лэйнам, с которыми совпадает orientation
                // TODO понять, всегда ли right полосы с "-"
                if (signal.orientation == "-") {
                    for (lane in lanes.filter { it.laneId < 0 }) {
                        lane.signal = Signal(signal, troad, lane.laneId)
                    }
                } else {
                    for (lane in lanes.filter { it.laneId > 0 }) {
                        lane.signal = Signal(signal, troad, lane.laneId)
                    }
                }
            }
        }
    }

    fun getLaneById(laneId: Int): Lane {
        val lane = lanes.filter {it.laneId == laneId}
        assert(lane.size == 1)

        return lane.first()
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

