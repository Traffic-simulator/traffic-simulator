package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.editor.Holder
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Building
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.DEFAULT_INTERSECTION_PADDING

class AddIntersectionStateChange(
    val position: Vec3,
    val building: Building? = null,
    val holder: Holder<Intersection>? = null
) : IStateChange {
    var newIntersection: Intersection? = null

    override fun apply(layout: Layout) {
        val intersectionId = layout.intersectionIdCount++
        val intersection =
            Intersection(
                intersectionId, position.xzProjection(), DEFAULT_INTERSECTION_PADDING,
                building
            )
        layout.intersections[intersectionId] = intersection

        holder?.let { it.obj = intersection }

        newIntersection = intersection
    }

    override fun revert(layout: Layout) {
        newIntersection?.let {
            layout.intersections.remove(it.id)
        }
    }
}
