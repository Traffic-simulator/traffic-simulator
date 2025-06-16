package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class AddRoadStateChange(
    private val startPoint: Pair<Vec3, Vec3>,
    private val startIntersection: Intersection?,
    private val endPoint: Pair<Vec3, Vec3>,
    private val endIntersection: Intersection?
) : IStateChange {
    private var newRoad: Road? = null

    private val startStateChange = AddIntersectionStateChange(startPoint.first)
    private val endStateChange = AddIntersectionStateChange(endPoint.first)

    override fun apply(layout: Layout) {
        if (newRoad == null) {
            val realStartIntersection = startIntersection ?: startStateChange.apply(layout)
            val realEndIntersection = endIntersection ?: endStateChange.apply(layout)

            if (realStartIntersection.position.distance(realEndIntersection.position) <
                realStartIntersection.padding + realEndIntersection.padding) {
                endStateChange.revert(layout)
                startStateChange.revert(layout)
                throw IllegalArgumentException("StartPadding + EndPadding > road.length")
            }

            newRoad = layout.addRoad(
                realStartIntersection, startPoint.second,
                realEndIntersection, endPoint.second
            )
        } else {
            layout.addRoad(newRoad!!)
        }
    }

    override fun revert(layout: Layout) {
        if (newRoad != null) {
            layout.deleteRoad(newRoad!!)

            endStateChange.revert(layout)
            startStateChange.revert(layout)
        }
    }
}
