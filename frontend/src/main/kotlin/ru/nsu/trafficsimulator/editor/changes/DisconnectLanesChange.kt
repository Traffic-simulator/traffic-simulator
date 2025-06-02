package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.IntersectionRoad
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class DisconnectLanesChange(
    private val intersection: Intersection,
    private val fromRoad: Road,
    private val fromLane: Int,
    private val toRoad: Road,
    private val toLane: Int
) : IStateChange {
    private var deletedIntersectionRoad: IntersectionRoad? = null

    override fun apply(layout: Layout) {
        deletedIntersectionRoad = intersection.disconnectLanes(
            fromRoad,
            fromLane,
            toRoad,
            toLane
        )
    }

    override fun revert(layout: Layout) {
        deletedIntersectionRoad?.let {
            intersection.pushIntersectionRoad(it)
        }
    }
}
