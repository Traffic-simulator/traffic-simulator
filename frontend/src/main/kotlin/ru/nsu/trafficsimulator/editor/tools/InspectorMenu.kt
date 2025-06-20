package ru.nsu.trafficsimulator.editor.tools

import imgui.ImGui
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2

interface InspectorItem<T> {
    fun runImguiItem(subj: T): IStateChange?
}

class InspectorMenuBuilder<T>(private val name: String) {
    private val items: MutableList<InspectorItem<T>> = mutableListOf()
    private val filters: MutableList<(subj: T) -> Boolean> = mutableListOf()

    fun withFilter(filter: (subj: T) -> Boolean): InspectorMenuBuilder<T> {
        filters.add(filter)
        return this
    }

    fun withItem(title: String, text: (subj: T) -> String): InspectorMenuBuilder<T> {
        items.add(object : InspectorItem<T> {
            override fun runImguiItem(subj: T): IStateChange? {
                ImGui.tableNextRow()

                ImGui.tableSetColumnIndex(0)
                ImGui.text(title)
                ImGui.tableSetColumnIndex(1)
                ImGui.text(text(subj))

                return null
            }
        })
        return this
    }

    fun withCustomItem(title: String, imgui: (subj: T) -> IStateChange?): InspectorMenuBuilder<T> {
        items.add(object: InspectorItem<T> {
            override fun runImguiItem(subj: T): IStateChange? {
                ImGui.tableNextRow()

                ImGui.tableSetColumnIndex(0)
                ImGui.text(title)
                ImGui.tableSetColumnIndex(1)
                return imgui(subj)
            }
        })

        return this
    }

    fun finish(): InspectorMenu<T> {
        return InspectorMenu(name, items, filters)
    }
}

class InspectorMenu<T>(
    private val name: String,
    private val items: List<InspectorItem<T>>,
    private val filters: List<(subj: T) -> Boolean>,
) {

    fun fits(subject: T): Boolean {
        for (filter in filters) {
            if (!filter(subject)) {
                return false
            }
        }

        return true
    }

    fun runImgui(subject: T, pos: Vec2?): IStateChange? {
        if (pos != null) {
            ImGui.setNextWindowPos(pos.x.toFloat(), pos.y.toFloat())
        }
        ImGui.begin("$name settings")

        if (!ImGui.beginTable("##${name}", 2)) {
            ImGui.end()
            return null
        }

        var change: IStateChange? = null
        for (item in items) {
            val itemRes = item.runImguiItem(subject)
            change = change ?: itemRes
        }

        ImGui.endTable()
        ImGui.end()

        return change
    }
}
