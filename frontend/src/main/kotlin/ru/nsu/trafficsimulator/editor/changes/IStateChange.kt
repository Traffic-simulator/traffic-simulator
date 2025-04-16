package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout

interface IStateChange {
    fun apply(layout: Layout)
    fun revert(layout: Layout)
}
