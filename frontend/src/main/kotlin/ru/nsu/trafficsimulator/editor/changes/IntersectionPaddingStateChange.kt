package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class IntersectionPaddingStateChange
    (private val intersection: Intersection, private val padding: Double) : IStateChange {
    private val prevPadding = intersection.padding

    override fun apply(layout: Layout) {
        intersection.padding = padding
    }

    override fun revert(layout: Layout) {
        intersection.padding = prevPadding
    }
}
