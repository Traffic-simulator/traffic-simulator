package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.editor.Holder
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
        val realStartIntersection: Intersection = if (startIntersection == null) {
            val startHolder = Holder<Intersection>()
            startStateChange = AddIntersectionStateChange(startPoint.first, holder = startHolder)
            startStateChange?.apply(layout)
            startHolder.obj!!
        } else {
            startIntersection
        }

        val realEndIntersection: Intersection = if (endIntersection == null) {
            val endHolder = Holder<Intersection>()
            endStateChange = AddIntersectionStateChange(endPoint.first, holder = endHolder)
            endStateChange?.apply(layout)
            endHolder.obj!!
        } else {
            endIntersection
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
