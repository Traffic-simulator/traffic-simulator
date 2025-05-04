package ru.nsu.trafficsimulator.editor.changes

import imgui.type.ImInt
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class EditRoadStateChange(
    private val layout: Layout,
    private val road: Road,
    private var currentLeftLines: ImInt?,
    private var currentRightLines: ImInt?
) :
    IStateChange {

    override fun apply(layout: Layout) {
        layout.roadSetLaneNumber(
            road,
            leftLane = currentLeftLines?.get() ?: 1,
            rightLane = currentRightLines?.get() ?: 1
        )
    }

    override fun revert(layout: Layout) {
        TODO("Not yet implemented")
    }
}
