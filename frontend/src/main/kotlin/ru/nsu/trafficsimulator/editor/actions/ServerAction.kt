package ru.nsu.trafficsimulator.editor.actions

import OpenDriveReader
import imgui.ImGui
import imgui.type.ImString
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.server.Server

const val PORT = 8080

class ServerAction : IAction {
    private val filename1 = ImString("template_1.xodr")
    private val filename2 = ImString("template_2.xodr")
    private val filename3 = ImString("template_3.xodr")
    private val filename4 = ImString("template_4.xodr")

    override fun isStructuralAction(): Boolean = true

    override fun runImgui(): Boolean {
        ImGui.text("Select the district layout:")

        ImGui.text("Template 1:")
        ImGui.sameLine()
        ImGui.inputText("##tmp1", filename1)

        ImGui.text("Template 2:")
        ImGui.sameLine()
        ImGui.inputText("##tmp2", filename2)

        ImGui.text("Template 3:")
        ImGui.sameLine()
        ImGui.inputText("##tmp3", filename3)

        ImGui.text("Template 4:")
        ImGui.sameLine()
        ImGui.inputText("##tmp4", filename4)

        val res = ImGui.button("Create host")
        return res
    }

    override fun runAction(layout: Layout): Boolean {
        try {
            val hostLayouts = hostLayouts()

            val server = Server(PORT, hostLayouts)
            val resultLayout = server.start()
            layout.copy(resultLayout)
            return true
        } catch (e: Exception) {
            logger.error(e) { e.message }
            return false
        }
    }

    private fun hostLayouts(): Map<Int, Layout> {
        return listOf(
            1 to Deserializer.deserialize(OpenDriveReader().read(filename1.toString())),
            2 to Deserializer.deserialize(OpenDriveReader().read(filename2.toString())),
            3 to Deserializer.deserialize(OpenDriveReader().read(filename3.toString())),
            4 to Deserializer.deserialize(OpenDriveReader().read(filename4.toString())),
        ).associate { it.first to it.second }
    }
}
