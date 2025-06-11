package ru.nsu.trafficsimulator.model

import kotlin.math.max

class Signal {
    var redOffsetOnStartSecs = DEFAULT_RED_OFFSET_ON_START
        set(value) {
            field = clampOffsetTime(value)
        }
    var redTimeSecs = DEFAULT_RED_TIME
        set(value) {
            field = clampSignalTime(value)
        }
    var greenTimeSecs = DEFAULT_GREEN_TIME
        set(value) {
            field = clampSignalTime(value)
        }

    override fun toString(): String {
        return "Signal(redOffset=${redOffsetOnStartSecs}, redTime=${redTimeSecs}, greenTime=${greenTimeSecs})"
    }

    companion object {
        fun clampOffsetTime(value: Int): Int {
            return max(value, 0)
        }

        fun clampSignalTime(value: Int): Int {
            return max(value, MIN_SIGNAL_TIME)
        }

        private const val DEFAULT_RED_OFFSET_ON_START: Int = 0
        private const val DEFAULT_RED_TIME: Int = 10
        private const val DEFAULT_GREEN_TIME: Int = 10
        private const val MIN_SIGNAL_TIME: Int = 5
    }
}
