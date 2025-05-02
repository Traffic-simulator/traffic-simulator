package ru.nsu.trafficsimulator.editor.actions

import OpenDriveReader
import imgui.ImGui
import imgui.type.ImString
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.Deserializer

class LoadAction : IAction {
    private val str = ImString()
    private var errorMessage = ""
    override fun runImgui(): Boolean {
        ImGui.text("Filename:")
        ImGui.sameLine()
        ImGui.inputText("##load", str)
        ImGui.sameLine()
        val res = ImGui.button("Load")
        if (errorMessage.isNotEmpty()) {
            ImGui.text(errorMessage)
        }
        return res;
    }

    override fun runAction(layout: Layout): Boolean {
        try {
            val opendrive = OpenDriveReader().read(str.toString())
            val newLayout = Deserializer.deserialize(opendrive)
            layout.copy(newLayout)
            return true
        } catch (e: Exception) {
            errorMessage = "Failed to load layout: $e"
            return false
        } catch (e: Error) {
            errorMessage = "Failed to load layout: $e"
            return false
        }
    }
}
