package ru.nsu.trafficsimulator.editor

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class DeleteRoadStateChange(private val road: Road) : IStateChange {
    private val start = road.startIntersection!!
    private val startDir = start.position + road.getDirection(0.0)
    private val end = road.endIntersection!!
    private val endDir = end.position + road.getDirection(road.length)

    override fun apply(layout: Layout) {
        layout.deleteRoad(road)
    }

    override fun revert(layout: Layout) {
        layout.addRoad(start, startDir, end, endDir)
    }
}
