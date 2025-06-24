package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class EditRoadSpeedLimitStateChange(private val road: Road, private val newSpeedLimit: Double) : IStateChange {
    override fun isStructuralChange() = false

    private val prevSpeedLimit = road.maxSpeed
    override fun apply(layout: Layout) {
        road.maxSpeed = newSpeedLimit
    }

    override fun revert(layout: Layout) {
        road.maxSpeed = prevSpeedLimit
    }
}
