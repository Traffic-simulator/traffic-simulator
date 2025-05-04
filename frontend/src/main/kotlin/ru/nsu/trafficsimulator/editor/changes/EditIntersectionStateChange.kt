package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class EditIntersectionStateChange
    (val intersection: Intersection, val padding: Double) : IStateChange {
    override fun apply(layout: Layout) {
        intersection.padding = padding
    }

    override fun revert(layout: Layout) {
        TODO("Not yet implemented")
    }
}
