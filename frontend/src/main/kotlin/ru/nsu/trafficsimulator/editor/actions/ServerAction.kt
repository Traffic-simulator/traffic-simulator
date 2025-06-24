package ru.nsu.trafficsimulator.editor.actions

import OpenDriveReader
import imgui.ImGui
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.server.Server
import java.util.HashMap
import java.util.TreeMap

const val PORT = 8080

class ServerAction : IAction {
    override fun isStructuralAction(): Boolean = true

    override fun runImgui(): Boolean {
        val res = ImGui.button("Host")
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
            Deserializer.deserialize(OpenDriveReader().read("export/client-$it.xodr"))
        }
    }
}
