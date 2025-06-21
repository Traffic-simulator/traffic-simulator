package ru.nsu.trafficsimulator.editor.actions

import imgui.ImGui
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.intsettings.MergingIntersectionSettings
import ru.nsu.trafficsimulator.server.Client
import ru.nsu.trafficsimulator.server.Server

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

    private fun hostLayout(): Layout {
        val result = Layout(0)
        result.addIntersection(Vec3(150.0, 0.0, 0.0), MergingIntersectionSettings(4, 1))
        result.addIntersection(Vec3(0.0, 0.0, 150.0), MergingIntersectionSettings(1, 2))
        result.addIntersection(Vec3(-150.0, 0.0, 0.0), MergingIntersectionSettings(2, 3))
        result.addIntersection(Vec3(0.0, 0.0, -150.0), MergingIntersectionSettings(3, 4))

        return result
    }
}
