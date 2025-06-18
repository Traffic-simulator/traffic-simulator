package ru.nsu.trafficsimulator.editor.actions

import imgui.ImGui
import imgui.type.ImString
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.server.Client

class ClientAction() : IAction {
    private val address = ImString()
    private val port = ImString()
    private val client = Client()
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
            val newLayout = client.connect(address, port)
            println(newLayout.toString())
            return true
        } catch (e: Exception) {
            println(e.message)
            return false
        }
    }
}
