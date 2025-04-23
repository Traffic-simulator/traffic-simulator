package network.signals;

import opendrive.TRoad
import opendrive.TRoadSignalsSignal;
import signals.SignalState

class Signal(val tsignal: TRoadSignalsSignal, tRoad: TRoad, lane: Int) {
    val road: TRoad = tRoad
    val laneId: Int = lane
    val s = tsignal.s
    val t = tsignal.t
    val orientation: String = tsignal.orientation
    val dynamic: String = tsignal.dynamic.value()
    //
    val cycle: String = tsignal.subtype

    var stateChangeTimer = 0.0
    var timeRed = 0.0
    var timeGreen = 0.0

    var nextState = SignalState.GREEN
    var changingStateTime = 0.5

    var state: SignalState = SignalState.RED
        get() {
            return field
        }

    init {
        stateChangeTimer = cycle.split("-")[0].toDouble() * 1000 // time before first change
        timeRed = cycle.split("-")[1].toDouble() * 1000
        timeGreen = cycle.split("-")[2].toDouble() * 1000
    }

    fun getNewTimer(nextState: SignalState) : Double {
        return when (nextState) {
            SignalState.RED -> timeRed
            SignalState.GREEN -> timeGreen
            else -> timeRed
        }
    }

    fun updateState(deltaTime: Double) {
        stateChangeTimer -= deltaTime

        if (stateChangeTimer < 0) {
            // change to next state
            state = nextState
            println("Changing color from ${state} TO ${nextState}")
            nextState = nextState.invert()
            println("New nextState is ${nextState}")
            stateChangeTimer = getNewTimer(nextState)
            return
        }

        if (stateChangeTimer < changingStateTime) {
            // change to changingStateColor
            if (nextState == SignalState.GREEN) {
                state = SignalState.RED_YELLOW  // from red to green through this one
            } else {
                state = SignalState.YELLOW  // from green to red through this
            }
        }
    }
}
