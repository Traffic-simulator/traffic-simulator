package ru.nsu.trafficsimulator.editor.actions

import OpenDriveReader
import OpenDriveWriter
import imgui.ImGui
import imgui.type.ImString
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.serializer.serializeLayout

class SaveAction : IAction {
    private val str = ImString()
    private var errorMessage = ""
    override fun runImgui(): Boolean {
        ImGui.text("Filename:")
        ImGui.sameLine()
        ImGui.inputText("##save", str)
        ImGui.sameLine()
        val res = ImGui.button("Save")
        if (errorMessage.isNotEmpty()) {
            ImGui.text(errorMessage)
        }
        return res;
    }

    override fun runAction(layout: Layout): Boolean {
        try {
            val opendrive = serializeLayout(layout)
            OpenDriveWriter().write(opendrive, str.toString())
            return false
        } catch (e: Exception) {
            errorMessage = "Failed to save layout: $e"
            return false
        } catch (e: Error) {
            errorMessage = "Failed to save layout: $e"
            return false
        }
    }
}
