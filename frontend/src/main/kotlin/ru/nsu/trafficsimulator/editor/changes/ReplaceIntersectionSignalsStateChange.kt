package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Signal

class ReplaceIntersectionSignalsStateChange(private val intersection: Intersection, private val signals: HashMap<Road, Signal>) : IStateChange {
    private val prevSignals = HashMap(intersection.signals)

    override fun isStructuralChange() = true

    override fun apply(layout: Layout) {
        intersection.signals = signals
    }

    override fun revert(layout: Layout) {
        intersection.signals = prevSignals
    }
}
