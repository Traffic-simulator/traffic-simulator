package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.editor.logger

class Signal {
    var redOffsetOnStartSecs = DEFAULT_RED_OFFSET_ON_START
        set(value) {
            if (value >= 0) {
                field = value
            } else {
                logger.warn("Tried to set start offset for traffic light = $value. This did not take effect")
            }
        }
    var redTimeSecs = DEFAULT_RED_TIME
        set(value) {
            if (value - MIN_SIGNAL_TIME >= 0) {
                field = value
            } else {
                logger.warn("Tried to set red duration for traffic light = $value. This did not take effect")
            }
        }
    var greenTimeSecs = DEFAULT_GREEN_TIME
        set(value) {
            if (value - MIN_SIGNAL_TIME >= 0) {
                field = value
            } else {
                logger.warn("Tried to set green duration for traffic light = $value. This did not take effect")
            }
        }

    companion object {
        private const val DEFAULT_RED_OFFSET_ON_START: Long = 0
        private const val DEFAULT_RED_TIME: Long = 10
        private const val DEFAULT_GREEN_TIME: Long = 10
        private const val MIN_SIGNAL_TIME: Long = 5
    }
}
