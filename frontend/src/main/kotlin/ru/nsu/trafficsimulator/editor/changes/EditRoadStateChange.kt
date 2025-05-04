package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class EditRoadStateChange(
    private val road: Road,
    private var currentLeftLines: Int,
    private var currentRightLines: Int
) : IStateChange {
    private val prevLeftLaneCnt = road.leftLane
    private val prevRightLaneCnt = road.rightLane

    override fun apply(layout: Layout) {
        layout.roadSetLaneNumber(
            road,
            leftLane = currentLeftLines,
            rightLane = currentRightLines
        )
    }

    override fun revert(layout: Layout) {
        layout.roadSetLaneNumber(
            road,
            leftLane = prevLeftLaneCnt,
            rightLane = prevRightLaneCnt
        )
    }
}
