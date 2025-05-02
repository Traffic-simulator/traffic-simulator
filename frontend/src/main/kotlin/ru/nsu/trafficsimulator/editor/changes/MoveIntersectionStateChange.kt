package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.math.Vec3

class MoveIntersectionStateChange(private val intersection: Intersection, private val newPos: Vec3) : IStateChange {
    private val prevPos = intersection.position
    override fun apply(layout: Layout) {
        layout.moveIntersection(intersection, newPos)
    }

    override fun revert(layout: Layout) {
        layout.moveIntersection(intersection, prevPos)
    }

}
