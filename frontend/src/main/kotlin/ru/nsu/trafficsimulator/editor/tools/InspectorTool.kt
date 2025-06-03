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
import ru.nsu.trafficsimulator.editor.changes.EditBuildingStateChange
import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.findRoadIntersectionAt
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.BuildingType
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

    private val incomingLanes = mutableListOf<LaneSphere>()

    private val outgoingLanesSphere = mutableListOf<OutgoingLaneSphere>()
    private var selectedFromRoad: LaneSphere? = null
    private var connectLanesChange: ConnectLanesChange? = null
    private var disconnectLanesChange: DisconnectLanesChange? = null


    override fun getButtonName(): String = name

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        lastClickPos = screenPos

        if (incomingLanes.isNotEmpty()) {
            getIntersectionWithGround(screenPos, camera)?.let { groundPoint ->
                selectedIntersection?.let { intersection ->
                    if (isNear(groundPoint, intersection, Layout.LANE_WIDTH / 2) { it.position.toVec3() }) {
                        incomingLanes.clear()
                        outgoingLanesSphere.clear()
                        selectedFromRoad = null
                        connectLanesChange = null
                        disconnectLanesChange = null

                        drawIncomingConnections(intersection)

                        return true
                    }
                }

                if (outgoingLanesSphere.isEmpty()) {
                    // only incoming lines are displayed

                    selectedFromRoad = findNearestObject(groundPoint, incomingLanes, Layout.LANE_WIDTH / 2) {
                        it.model.transform.getTranslation(Vector3()).toVec3()
                    }

                    selectedFromRoad?.also {
                        incomingLanes.clear()
                        incomingLanes.add(it)
                        drawOutgoingConnections(it.intersection)

                    } ?: cleanUpIntersectionRoadsSettings()

                    return true
                } else {
                    // one incoming and all outgoing lines are displayed

                    val toSwitch = findNearestObject(groundPoint, outgoingLanesSphere, Layout.LANE_WIDTH / 2) {
                        it.transform.getTranslation(Vector3()).toVec3()
                    }

                    toSwitch?.also { toRoad ->
                        selectedFromRoad?.let { fromRoad ->
                            selectedIntersection?.let { intersection ->
                                if (intersection.findConnectingRoad(
                                        fromRoad.road,
                                        fromRoad.lane,
                                        toRoad.road,
                                        toRoad.lane
                                    ) == null
                                ) {
                                    connectLanesChange = ConnectLanesChange(
                                        fromRoad.intersection,
                                        fromRoad.road,
                                        fromRoad.lane,
                                        toRoad.road,
                                        toRoad.lane
                                    )
                                } else {
                                    disconnectLanesChange = DisconnectLanesChange(
                                        fromRoad.intersection,
                                        fromRoad.road,
                                        fromRoad.lane,
                                        toRoad.road,
                                        toRoad.lane
                                    )
                                }
                            }
                        }
                    } ?: cleanUpIntersectionRoadsSettings()

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
        connectLanesChange?.let {
            connectLanesChange = null
            return it
        }
        disconnectLanesChange?.let {
            disconnectLanesChange = null
            return it
        }
        return null
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch) {
        selectedFromRoad?.let { fromRoad ->
            for (sphere in outgoingLanesSphere) {
                modelBatch.render(sphere.getModel(fromRoad.road, fromRoad.lane))
            }
        }

        for (sphere in incomingLanes) {
            modelBatch.render(sphere.model)
        }
        return
    }

    override fun runImgui(): IStateChange? {
        if (selectedRoad != null) {
            return runRoadMenu(selectedRoad!!)
        } else if (selectedIntersection != null) {
            return if (selectedIntersection!!.isBuilding) {
                runBuildingMenu(selectedIntersection!!)
            } else {
                runIntersectionMenu(selectedIntersection!!)
            }
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

    private fun runBuildingMenu(intersection: Intersection): IStateChange? {
        if (lastClickPos != null) {
            ImGui.setNextWindowPos(lastClickPos!!.x.toFloat(), lastClickPos!!.y.toFloat())
            lastClickPos = null
        }

        ImGui.begin("Building settings")
        val buildingCapacity = ImInt(intersection.building!!.capacity)
        val types = arrayOf(
            "Home",
            "Shopping",
            "Education",
            "Work",
            "Entertainment"
        )
        val selectedType = ImInt(intersection.building!!.type.ordinal)
        if (ImGui.beginTable("##Building", 2)) {
            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Building capacity")
            ImGui.tableSetColumnIndex(1)
            if (ImGui.inputInt("##capacity", buildingCapacity)) {
                buildingCapacity.set(buildingCapacity.get().coerceIn(0, 1000))
            }

            ImGui.tableNextRow()
            ImGui.tableSetColumnIndex(0)
            ImGui.text("Building type")
            ImGui.tableSetColumnIndex(1)
            ImGui.combo("##type", selectedType, types)

            ImGui.endTable()
        }
        ImGui.end()

        if (buildingCapacity.get() != intersection.building!!.capacity || types[selectedType.get()].uppercase() != intersection.building!!.type.toString()) {
            return EditBuildingStateChange(
                intersection,
                buildingCapacity.get(),
                types[selectedType.get()].uppercase()
            )
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
            return IntersectionPaddingStateChange(intersection, padding.get())
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
                    LaneSphere(
                        intersection,
                        road,
                        i * road.getIncomingLaneNumber(intersection).sign,
                        sphereInstance
                    )
                )
            }
        }
    }

    private fun drawOutgoingConnections(intersection: Intersection) {
        outgoingLanesSphere.clear()
        for (road in intersection.incomingRoads) {
            for (i in 1..abs(road.getOutgoingLaneNumber(intersection))) {
                val realLaneNumber = i * road.getOutgoingLaneNumber(intersection).sign

                outgoingLanesSphere.add(
                    OutgoingLaneSphere(intersection, road, realLaneNumber)
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

    private fun cleanUpIntersectionRoadsSettings() {
        incomingLanes.clear()
        outgoingLanesSphere.clear()
        selectedFromRoad = null
        selectedIntersection = null
        connectLanesChange = null
        disconnectLanesChange = null
    }

    private fun Vector3.toVec3() = Vec3(x, y, z)

    private data class LaneSphere(
        val intersection: Intersection,
        val road: Road,
        val lane: Int,
        val model: ModelInstance
    )


    private data class OutgoingLaneSphere(
        val intersection: Intersection, val road: Road, val lane: Int
    ) {
        private val turnModel: ModelInstance
        private val offModel: ModelInstance

        val transform
            get() = turnModel.transform

        init {
            val turnInstance = ModelInstance(createSphere(Color.CORAL, 1.0))
            val offInstance = ModelInstance(createSphere(Color.BROWN, 1.0))
            val position = road.getIntersectionPoint(intersection, -(abs(lane).toDouble() - 0.5))
            position.y = 1.0

            turnInstance.transform.setToTranslation(position.toGdxVec())
            offInstance.transform.setToTranslation(position.toGdxVec())

            turnModel = turnInstance
            offModel = offInstance
        }

        fun getModel(fromRoad: Road, fromLane: Int): ModelInstance {
            if (intersection.findConnectingRoad(fromRoad, fromLane, road, lane) == null) {
                return offModel
            }
            return turnModel
        }
    }
}
