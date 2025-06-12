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

    private var startStateChange: AddIntersectionStateChange? = null
    private var endStateChange: AddIntersectionStateChange? = null

    override fun apply(layout: Layout) {
        if (newRoad != null) {
            return
        }

        val realStartIntersection: Intersection = if (startIntersection == null) {
            startStateChange = AddIntersectionStateChange(startPoint.first)
            startStateChange?.apply(layout)!!
        } else {
            startIntersection
        }

        val realEndIntersection: Intersection = if (endIntersection == null) {
            endStateChange = AddIntersectionStateChange(endPoint.first)
            endStateChange?.apply(layout)!!
        } else {
            endIntersection
        }

        if (realStartIntersection.position.distance(realEndIntersection.position) <
            realStartIntersection.padding + realEndIntersection.padding) {
            endStateChange?.revert(layout)
            startStateChange?.revert(layout)
            throw IllegalArgumentException("StartPadding + EndPadding > road.length")
        }

        newRoad = layout.addRoad(
            realStartIntersection, startPoint.second,
            realEndIntersection, endPoint.second
        )

    }

    override fun revert(layout: Layout) {
        if (newRoad != null) {
            layout.deleteRoad(newRoad!!)
        }

        endStateChange?.revert(layout)
        startStateChange?.revert(layout)
    }
}
