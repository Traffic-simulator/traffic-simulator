package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCond
import imgui.type.ImInt
import ru.nsu.trafficsimulator.editor.changes.ChangeSignalStateChange
import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.findRoadIntersectionAt
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Signal

class InspectorTool() : IEditingTool {
    private val name = "Inspector"
    private var layout: Layout? = null
    private var camera: Camera? = null

    // TODO: hold a variant somehow? Maybe make abstract class Inspector and override for each primitive?
    private var selectedRoad: Road? = null
    private var selectedIntersection: Intersection? = null
    private var lastClickPos: Vec2? = null

    override fun getButtonName(): String = name

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        lastClickPos = screenPos
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false
        selectedIntersection = findRoadIntersectionAt(layout!!, intersection)
        if (selectedIntersection != null) {
            selectedRoad = null
            return true
        }
        selectedRoad = findRoad(layout!!, intersection)
        if (selectedRoad != null) {
            selectedIntersection = null
            return true
        }
        return false
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
        } else if (selectedIntersection != null) {
            return runIntersectionMenu(selectedIntersection!!)
        }
        return null
    }

    private fun runRoadMenu(road: Road): IStateChange? {
        if (lastClickPos != null) {
            ImGui.setNextWindowPos(lastClickPos!!.x.toFloat(), lastClickPos!!.y.toFloat())
            lastClickPos = null
        }
        ImGui.begin("Road settings")

        val leftLaneCnt = ImInt(road.leftLane)
        val rightLaneCnt = ImInt(road.rightLane)
        if (ImGui.beginTable("##Road", 2)) {
            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("ID")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(road.id.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Start junction ID")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(road.startIntersection.id.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("End junction ID")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(road.endIntersection.id.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Left lane count")
            ImGui.tableSetColumnIndex(1)
            if (ImGui.inputInt("##left", leftLaneCnt)) {
                leftLaneCnt.set(leftLaneCnt.get().coerceIn(Road.MIN_LANE_COUNT, Road.MAX_LANE_COUNT))
            }

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Right lane count")
            ImGui.tableSetColumnIndex(1)
            if (ImGui.inputInt("##right", rightLaneCnt)) {
                rightLaneCnt.set(rightLaneCnt.get().coerceIn(Road.MIN_LANE_COUNT, Road.MAX_LANE_COUNT))
            }

            ImGui.endTable()
        }
        ImGui.textDisabled("Acceptable range: ${Road.MIN_LANE_COUNT}..${Road.MAX_LANE_COUNT}")
        ImGui.end()

        if (leftLaneCnt.get() != road.leftLane || rightLaneCnt.get() != road.rightLane) {
            return EditRoadStateChange(road, leftLaneCnt.get(), rightLaneCnt.get())
        }

        return null
    }

    private fun runIntersectionMenu(intersection: Intersection): IStateChange? {
        if (lastClickPos != null) {
            ImGui.setNextWindowPos(lastClickPos!!.x.toFloat(), lastClickPos!!.y.toFloat())
            lastClickPos = null
        }
        ImGui.begin("Intersection settings")

        var stateChange: IStateChange? = null
        if (ImGui.beginTable("##Intersection", 2)) {
            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("ID")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(intersection.id.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Incoming roads IDs")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(intersection.incomingRoads.map{ it.id }.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Inner roads IDs")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(intersection.intersectionRoads.map{ it.id }.toString())

            if (intersection.hasSignals) {
                for (road in intersection.incomingRoads) {
                    ImGui.pushID(road.id)
                    ImGui.tableNextRow()
                    ImGui.tableSetColumnIndex(0)
                    ImGui.text("Signal for road #${road.id}")
                    ImGui.tableSetColumnIndex(1)
                    val signal = intersection.signals[road]
                    if (signal == null) {
                        ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "ERROR: no signal found")
                    } else {
                        var currentOffset = ImInt(signal.redOffsetOnStartSecs)
                        var currentRed = ImInt(signal.redTimeSecs)
                        var currentGreen = ImInt(signal.greenTimeSecs)
                        ImGui.pushItemWidth(80.0f)
                        if (ImGui.inputInt("##offset", currentOffset)) {
                            currentOffset.set(Signal.clampOffsetTime(currentOffset.get()))
                        }
                        ImGui.sameLine()
                        if (ImGui.inputInt("##red", currentRed)) {
                            currentRed.set(Signal.clampSignalTime(currentRed.get()))
                        }
                        ImGui.sameLine()
                        if (ImGui.inputInt("##green", currentGreen)) {
                            currentGreen.set(Signal.clampSignalTime(currentGreen.get()))
                        }
                        ImGui.popItemWidth()
                        if (currentOffset.get() != signal.redOffsetOnStartSecs
                            || currentRed.get() != signal.redTimeSecs
                            || currentGreen.get() != signal.greenTimeSecs
                            ) {
                            stateChange = ChangeSignalStateChange(signal, currentOffset.get(), currentRed.get(), currentGreen.get())
                        }
                    }
                    ImGui.popID()
                }
            }

            ImGui.endTable()
        }

        ImGui.end()
        return stateChange
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.camera = camera
        this.layout = layout
        if (reset) {
            selectedIntersection = null
            selectedRoad = null
        }
    }
}
