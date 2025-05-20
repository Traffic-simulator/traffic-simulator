package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout

interface IStateChange {
    /**
     * Simply: do we need to update mesh for layout or not?
     */
    fun isStructuralChange(): Boolean = true

    fun apply(layout: Layout)
    fun revert(layout: Layout)
}
