package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import imgui.ImGui
import imgui.type.ImDouble
import imgui.type.ImInt
import ru.nsu.trafficsimulator.editor.changes.*
import ru.nsu.trafficsimulator.editor.createSphere
import ru.nsu.trafficsimulator.math.*
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Signal
import kotlin.math.abs
import kotlin.math.sign

class InspectorTool : IEditingTool {
    private val name = "Inspector"
    private lateinit var layout: Layout
    private lateinit var camera: Camera

    // TODO: hold a variant somehow? Maybe make abstract class Inspector and override for each primitive?
    private var selectedRoad: Road? = null
    private var selectedIntersection: Intersection? = null
    private var lastClickPos: Vec2? = null

    private val incomingLanes = mutableListOf<Sphere>()
    private val outgoingLanesConnected = mutableListOf<Sphere>()
    private val outgoingLanesDisconnected = mutableListOf<Sphere>()
    private var selectedFromRoad: Sphere? = null
    private var intersectionRoadChanged = false


    override fun getButtonName(): String = name

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        lastClickPos = screenPos

        if (incomingLanes.isNotEmpty()) {
            getIntersectionWithGround(screenPos, camera)?.let { groundPoint ->
                if (outgoingLanesConnected.isEmpty() && outgoingLanesDisconnected.isEmpty()) {
                    selectedFromRoad = findNearestObject(groundPoint, incomingLanes, Layout.LANE_WIDTH / 2) {
                        it.model.transform.getTranslation(Vector3()).toVec3()
                    }

                    selectedFromRoad?.also {
                        incomingLanes.clear()
                        incomingLanes.add(it)
                        drawOutgoingConnections(it.intersection, it.road, it.lane)
                        intersectionRoadChanged = true

                    } ?: cleanUpIntersectionSettingsMenu()

                    return true
                } else {
                    val toDisconnect = findNearestObject(groundPoint, outgoingLanesConnected, Layout.LANE_WIDTH / 2) {
                        it.model.transform.getTranslation(Vector3()).toVec3()
                    }
                    val toConnect = if (toDisconnect != null) null else {
                        findNearestObject(groundPoint, outgoingLanesDisconnected, Layout.LANE_WIDTH / 2) {
                            it.model.transform.getTranslation(Vector3()).toVec3()
                        }
                    }

                    toDisconnect?.let {
                        selectedFromRoad?.intersection?.disconnectLanes(
                            selectedFromRoad?.road!!,
                            selectedFromRoad?.lane!!,
                            it.road,
                            it.lane
                        )
                        outgoingLanesConnected.remove(it)
                        val sphereModel = createSphere(Color.BROWN, 1.0)
                        val sphereInstance = ModelInstance(sphereModel)
                        sphereInstance.transform.setToTranslation(it.model.transform.getTranslation(Vector3()))
                        outgoingLanesDisconnected.add(it.copy(model = sphereInstance))
                        intersectionRoadChanged = true
                    }

                    toConnect?.let {
                        selectedFromRoad?.intersection?.connectLanes(
                            selectedFromRoad?.road!!,
                            selectedFromRoad?.lane!!,
                            it.road,
                            it.lane
                        )
                        outgoingLanesDisconnected.remove(it)
                        val sphereModel = createSphere(Color.CORAL, 1.0)
                        val sphereInstance = ModelInstance(sphereModel)
                        sphereInstance.transform.setToTranslation(it.model.transform.getTranslation(Vector3()))
                        outgoingLanesConnected.add(it.copy(model = sphereInstance))
                        intersectionRoadChanged = true
                    }

                    if (toDisconnect == null && toConnect == null)
                        cleanUpIntersectionSettingsMenu()
                    return true
                }

            }
        }

        val intersection = getIntersectionWithGround(screenPos, camera) ?: return false
        selectedIntersection = findRoadIntersectionAt(layout, intersection)
        if (selectedIntersection != null) {
            selectedRoad = null
            drawIncomingConnections(selectedIntersection!!)
            return true
        }
        selectedRoad = findRoad(layout, intersection)
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
        for (sphere in outgoingLanesConnected) {
            modelBatch!!.render(sphere.model)
        }

        for (sphere in outgoingLanesDisconnected) {
            modelBatch!!.render(sphere.model)
        }

        for (sphere in incomingLanes) {
            modelBatch!!.render(sphere.model)
        }
        return
    }

    override fun runImgui(): IStateChange? {
        if (intersectionRoadChanged) {
            return EditIntersectionConnectionChange()
        }
        if (selectedRoad != null) {
            return runRoadMenu(selectedRoad!!)
        }
        if (selectedIntersection != null) {
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
        ImGui.begin("Intersection settings")

        var stateChange: IStateChange? = null
        val padding = ImDouble(intersection.padding)
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
            ImGui.text(intersection.incomingRoads.map { it.id }.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Inner roads IDs")
            ImGui.tableSetColumnIndex(1)
            ImGui.text(intersection.intersectionRoads.keys.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Has signals")
            ImGui.tableSetColumnIndex(1)
            val hasSignalsChanged = ImGui.radioButton("##signals", intersection.hasSignals)
            if (hasSignalsChanged) {
                val newSignals = HashMap<Road, Signal>()
                if (!intersection.hasSignals) {
                    for (road in intersection.incomingRoads) {
                        newSignals[road] = Signal()
                    }
                }
                stateChange = ReplaceIntersectionSignalsStateChange(intersection, newSignals)
            }

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
                        val currentOffset = ImInt(signal.redOffsetOnStartSecs)
                        val currentRed = ImInt(signal.redTimeSecs)
                        val currentGreen = ImInt(signal.greenTimeSecs)
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
                            stateChange = ChangeSignalStateChange(
                                signal,
                                currentOffset.get(),
                                currentRed.get(),
                                currentGreen.get()
                            )
                        }
                    }
                    ImGui.popID()
                }
            }
            ImGui.text(intersection.intersectionRoads.keys.toString())

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Intersection padding")
            ImGui.tableSetColumnIndex(1)
            if (ImGui.inputDouble("##padding", padding)) {
                padding.set(padding.get().coerceIn(0.0, Double.MAX_VALUE))
            }

            ImGui.endTable()
        }

        ImGui.end()

        if (padding.get() != intersection.padding) {
            return EditIntersectionStateChange(intersection, padding.get())
        }

        return stateChange
    }

    private fun drawIncomingConnections(intersection: Intersection) {
        incomingLanes.clear()
        for (road in intersection.incomingRoads) {
            for (i in 1..abs(road.getIncomingLaneNumber(intersection))) {
                val sphereModel = createSphere(Color.GREEN, 1.0)
                val sphereInstance = ModelInstance(sphereModel)
                val position = road.getIntersectionPoint(intersection, (i.toDouble() - 0.5))
                position.y = 1.0
                sphereInstance.transform.setToTranslation(position.toGdxVec())
                incomingLanes.add(
                    Sphere(
                        intersection,
                        road,
                        i * road.getIncomingLaneNumber(intersection).sign,
                        sphereInstance
                    )
                )
            }
        }
    }

    private fun drawOutgoingConnections(intersection: Intersection, fromRoad: Road, fromLane: Int) {
        outgoingLanesConnected.clear()
        for (road in intersection.incomingRoads) {
            if (road === fromRoad) continue
            for (i in 1..abs(road.getOutgoingLaneNumber(intersection))) {
                val realLaneNumber = i * road.getOutgoingLaneNumber(intersection).sign

                val connected = intersection.findConnectingRoad(fromRoad, fromLane, road, realLaneNumber) != null

                val sphereModel = createSphere(if (connected) Color.CORAL else Color.BROWN, 1.0)
                val sphereInstance = ModelInstance(sphereModel)
                val position = road.getIntersectionPoint(intersection, -(i.toDouble() - 0.5))
                position.y = 1.0
                sphereInstance.transform.setToTranslation(position.toGdxVec())
                (if (connected) outgoingLanesConnected else outgoingLanesDisconnected).add(
                    Sphere(
                        intersection,
                        road,
                        realLaneNumber,
                        sphereInstance
                    )
                )
            }
        }
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.camera = camera
        this.layout = layout
        if (reset) {
            selectedIntersection = null
            selectedRoad = null
        }
    }

    private fun cleanUpIntersectionSettingsMenu() {
        incomingLanes.clear()
        outgoingLanesConnected.clear()
        outgoingLanesDisconnected.clear()
        selectedFromRoad = null
        selectedIntersection = null
        intersectionRoadChanged = false
    }

    private fun Vector3.toVec3() = Vec3(x, y, z)

    private data class Sphere(val intersection: Intersection, val road: Road, val lane: Int, val model: ModelInstance)
}
