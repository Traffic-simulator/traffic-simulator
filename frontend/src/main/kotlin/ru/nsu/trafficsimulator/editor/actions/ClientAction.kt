package ru.nsu.trafficsimulator.editor.actions

import imgui.ImGui
import imgui.type.ImString
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.server.Client

class ClientAction(private val client: Client?) : IAction {
    private val address = ImString("localhost")
    private val port = ImString("8080")
    override fun isStructuralAction(): Boolean = true

    override fun runImgui(): Boolean {
        ImGui.text("Server address:")
        ImGui.inputText("##address", address)

        ImGui.text("Port:")
        ImGui.inputText("##port", port)

        val res = ImGui.button("Connect")
        return res
    }

    override fun runAction(layout: Layout): Boolean {
        try {
            val address = address.toString()
            val port = port.toString().toInt()
            val newLayout = client!!.connect(address, port)
            println(newLayout.toString())
            layout.copy(newLayout)
            return true
        } catch (e: Exception) {
            logger.error(e) { e.message }
            return false
        }
    }
}
