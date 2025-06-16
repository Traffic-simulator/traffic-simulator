package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.BuildingIntersectionSettings
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.DEFAULT_INTERSECTION_PADDING

class AddIntersectionStateChange(
    val position: Vec3,
    val building: BuildingIntersectionSettings? = null,
) : IStateChange {
    private var newIntersection: Intersection? = null

    override fun apply(layout: Layout): Intersection {
        if (newIntersection == null) {
            val intersectionId = layout.intersectionIdCount++
            newIntersection =
                Intersection(
                    intersectionId, position.xzProjection(), DEFAULT_INTERSECTION_PADDING,
                    building
                )
        }

        layout.pushIntersection(newIntersection!!)

        return newIntersection!!
    }

    override fun revert(layout: Layout) {
        newIntersection?.let {
            layout.intersections.remove(it.id)
        }
    }
}
