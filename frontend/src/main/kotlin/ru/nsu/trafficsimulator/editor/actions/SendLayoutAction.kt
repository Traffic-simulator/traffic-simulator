package ru.nsu.trafficsimulator.editor.actions

import imgui.ImGui
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.server.Client

class SendLayoutAction(private val client: Client?) : IAction {
    override fun isStructuralAction(): Boolean = true

    override fun runImgui(): Boolean {
        val res = ImGui.button("Send layout")
        return res
    }

    override fun runAction(layout: Layout): Boolean {
        if (client!!.getConnected()) {
            try {
                val resLayout = client.sendLayout(layout)
                layout.copy(resLayout)
                return true
            } catch (e: Exception) {
                logger.error(e) { e.message }
                return false
            }
        }
        return false
    }
}
