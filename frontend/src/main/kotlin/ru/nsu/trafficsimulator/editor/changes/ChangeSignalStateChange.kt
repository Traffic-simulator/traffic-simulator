package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Signal

class ChangeSignalStateChange(private val signal: Signal, private val offset: Int, private val red: Int, private val green: Int) : IStateChange {
    private val prevOffset = signal.redOffsetOnStartSecs
    private val prevRed = signal.redTimeSecs
    private val prevGreen = signal.greenTimeSecs

    override fun apply(layout: Layout) {
        signal.redOffsetOnStartSecs = offset
        signal.redTimeSecs = red
        signal.greenTimeSecs = green
    }

    override fun revert(layout: Layout) {
        signal.redOffsetOnStartSecs = prevOffset
        signal.redTimeSecs = prevRed
        signal.greenTimeSecs = prevGreen
    }
}
