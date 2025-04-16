package network.signals;

import opendrive.TRoadSignalsSignal;

class Signal(val tsignal: TRoadSignalsSignal) {
    val id = tsignal.id
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
