package ru.nsu.trafficsimulator.editor.tools

import ru.nsu.trafficsimulator.editor.changes.IStateChange

class InspectorItem<T>(
    private val item: (subj: T) -> IStateChange?,
    private var filter: (subj: T) -> Boolean,
) {
    fun fits(subject: T): Boolean {
        return filter(subject)
    }

    fun run(subject: T): IStateChange? {
        return item(subject)
    }

    fun withFilter(filterFunc: (subj: T) -> Boolean): InspectorItem<T> {
        filter = filterFunc
        return this
    }
}
