package ru.nsu.trafficsimulator.editor

import imgui.type.ImInt
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class EditRoadStateChange(
    private val road: Road,
    private var currentLeftLines: ImInt?,
    private var currentRightLines: ImInt?
) :
    IStateChange {

    override fun apply(layout: Layout) {
        road.leftLane = currentLeftLines?.get() ?: 1
        road.rightLane = currentRightLines?.get() ?: 1

        println("road: ${road.id}, left: ${road.leftLane}, right: ${road.rightLane}")
    }

    override fun revert(layout: Layout) {
        TODO("Not yet implemented")
    }
}
