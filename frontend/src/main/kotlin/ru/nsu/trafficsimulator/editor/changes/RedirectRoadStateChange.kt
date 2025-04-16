package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.math.Vec3

class RedirectRoadStateChange(private val road: Road, private val newStartDir: Vec3, private val newEndDir: Vec3) : IStateChange {
    private val prevStartDir = road.startIntersection!!.position + road.getDirection(0.0)
    private val prevEndDir = road.endIntersection!!.position + road.getDirection(road.length)

    override fun apply(layout: Layout) {
        road.redirectRoad(
            road.startIntersection!!,
            newStartDir
        )
        road.redirectRoad(
            road.endIntersection!!,
            newEndDir
        )
    }

    override fun revert(layout: Layout) {
        road.redirectRoad(
            road.startIntersection!!,
            prevStartDir
        )
        road.redirectRoad(
            road.endIntersection!!,
            prevEndDir
        )
    }
}
