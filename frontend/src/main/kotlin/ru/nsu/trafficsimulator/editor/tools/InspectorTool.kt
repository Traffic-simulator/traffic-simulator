package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import imgui.ImGui
import imgui.type.ImInt
import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class InspectorTool() : IEditingTool {
    private val name = "Inspector"
    private var layout: Layout? = null
    private var camera: Camera? = null

    private var selectedRoad: Road? = null

    override fun getButtonName(): String = name

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false
        selectedRoad = findRoad(layout!!, intersection)
        return selectedRoad != null
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        return null
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch?) {
        return
    }

    override fun runImgui(): IStateChange? {
        if (selectedRoad != null) {
            return runRoadMenu(selectedRoad!!)
        }
        return null
    }

    private fun runRoadMenu(road: Road): IStateChange? {
        ImGui.begin("Road settings")

        if (ImGui.beginTable("##Road", 2)) {
            ImGui.tableNextRow()

            ImGui.tableSetColumnIndex(0)
            ImGui.text("Left lane count")
            ImGui.tableSetColumnIndex(1)
            val leftLaneCnt = ImInt(road.leftLane)
            if (ImGui.inputInt("##left", leftLaneCnt)) {
                leftLaneCnt.set(leftLaneCnt.get().coerceIn(Road.MIN_LANE_COUNT, Road.MAX_LANE_COUNT))
            }

            ImGui.tableNextRow()

            ImGui.tableSetColumnIndex(0)
            ImGui.text("Right lane count")
            ImGui.tableSetColumnIndex(1)
            val rightLaneCnt = ImInt(road.rightLane)
            if (ImGui.inputInt("##right", rightLaneCnt)) {
                rightLaneCnt.set(rightLaneCnt.get().coerceIn(Road.MIN_LANE_COUNT, Road.MAX_LANE_COUNT))
            }
            ImGui.endTable()
            ImGui.textDisabled("Acceptable range: ${Road.MIN_LANE_COUNT}..${Road.MAX_LANE_COUNT}")
            ImGui.end()

            if (leftLaneCnt.get() != road.leftLane || rightLaneCnt.get() != road.rightLane) {
                return EditRoadStateChange(road, leftLaneCnt.get(), rightLaneCnt.get())
            }
        }

        return null
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.camera = camera
        this.layout = layout
    }
}
