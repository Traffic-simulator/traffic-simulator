package ru.nsu.trafficsimulator.editor.actions

import OpenDriveReader
import imgui.ImGui
import imgui.type.ImString
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.server.Server
import java.util.HashMap

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
            val hostLayout = hostLayout()
            val server = Server(PORT, hostLayout)
            val resultLayout = server.start()
            layout.copy(resultLayout)
            return true
        } catch (e: Exception) {
            logger.error(e) { e.message }
            return false
        }
    }

    private fun hostLayout(): Map<Int, Layout> {
        return listOf(1, 2, 3, 4).associateWithTo(HashMap()) {
            Deserializer.deserialize(OpenDriveReader().read("template_$it.xodr"))
        }
    }
}
