package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.IntersectionRoad
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class ConnectLanesChange(
    private val intersection: Intersection,
    private val fromRoad: Road,
    private val fromLane: Int,
    private val toRoad: Road,
    private val toLane: Int
) : IStateChange {
    private var addedIntersectionRoad: IntersectionRoad? = null

    override fun apply(layout: Layout) {
        addedIntersectionRoad = intersection.connectLanes(
            fromRoad,
            fromLane,
            toRoad,
            toLane
        )
    }

    override fun revert(layout: Layout) {
        addedIntersectionRoad?.let {
            intersection.deleteIntersectionRoad(it)
        }
    }
}
