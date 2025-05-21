package network.signals;

import mu.KotlinLogging
import opendrive.TRoad
import opendrive.TRoadSignalsSignal;
import signals.SignalState

class Signal(val tsignal: TRoadSignalsSignal, tRoad: TRoad, lane: Int) {

    val logger = KotlinLogging.logger("BACKEND")

    val id = tsignal.id
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

    var nextState = SignalState.RED
    var changingStateTime = 0.5

    var state: SignalState = SignalState.RED
        get() {
            return field
        }

    init {
        stateChangeTimer = cycle.split("-")[0].toDouble()// time before first change
        timeRed = cycle.split("-")[1].toDouble()
        timeGreen = cycle.split("-")[2].toDouble()
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
            logger.info("TrafficLight@$id on road ${road.id} lane $laneId changed color from $state to $nextState")
            state = nextState
            stateChangeTimer = getNewTimer(state)

            nextState = nextState.invert()
            return
        }

        if (stateChangeTimer < changingStateTime) {
            // change to changingStateColor
            if (nextState == SignalState.GREEN && state != SignalState.RED_YELLOW) {
                state = SignalState.RED_YELLOW  // from red to green through this one
                logger.info("TrafficLight@$id on lane $laneId changed color from ${SignalState.RED} to ${SignalState.RED_YELLOW}")
            } else
            if (nextState == SignalState.RED && state != SignalState.YELLOW){
                state = SignalState.YELLOW  // from green to red through this
                logger.info("TrafficLight@$id on lane $laneId changed color from ${SignalState.GREEN} to ${SignalState.YELLOW}")
            }
        }
    }
}
