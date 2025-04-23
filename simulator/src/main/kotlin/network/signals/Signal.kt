package network.signals;

import opendrive.TRoad
import opendrive.TRoadLanes
import opendrive.TRoadSignalsSignal;
import signals.SignalState

class Signal(val tsignal: TRoadSignalsSignal, tRoad: TRoad, lane: Int) {
    val road: TRoad = tRoad
    val laneId: Int = lane
    val s = tsignal.s
    val t = tsignal.t
    val orientation: String = tsignal.orientation
    val dynamic: String = tsignal.dynamic.value()
    val cycle: String = tsignal.subtype

    val state: SignalState = SignalState.RED
        get() {
            return field
        }

    //TODO method for updating state
    // maybe with local counter of ms for changing it
}
