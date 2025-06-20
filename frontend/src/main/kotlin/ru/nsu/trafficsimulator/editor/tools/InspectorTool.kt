package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
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
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.findRoadIntersectionAt
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.*
import kotlin.math.abs
import kotlin.math.sign
import kotlin.reflect.KClass
import kotlin.reflect.full.*

class InspectorTool : IEditingTool {
    private val name = "Inspector"
    private lateinit var layout: Layout
    private lateinit var camera: Camera

    // TODO: hold a variant somehow? Maybe make abstract class Inspector and override for each primitive?
    private var selectedSubject: Any? = null
    private var lastClickPos: Vec2? = null

//    private val menuMap: MutableMap<KClass<*>, InspectorMenu<*>> = mutableMapOf()
    private val menus: MutableList<Pair<KClass<*>, InspectorMenu<*>>> = mutableListOf()

    private val incomingLanes = mutableListOf<LaneSphere>()

    private val outgoingLanesSphere = mutableListOf<OutgoingLaneSphere>()
    private var selectedFromRoad: LaneSphere? = null
    private var connectLanesChange: ConnectLanesChange? = null
    private var disconnectLanesChange: DisconnectLanesChange? = null

    init {
        registerMenu(
            InspectorMenuBuilder<Intersection>("Building")
                .withFilter { it.isBuilding }
                .withCustomItem("Building capacity") {
                    val capacity = ImInt(it.building!!.capacity)
                    if (ImGui.inputInt("##capacity", capacity)) {
                        EditBuildingStateChange(
                            it,
                            capacity.get().coerceIn(0, 1000),
                            it.building!!.type.toString(),
                        )
                    } else {
                        null
                    }
                }
                .withCustomItem("Building Type") {
                    val types = BuildingType.entries.map { it.name }.toTypedArray()
                    val prevType = it.building!!.type.ordinal
                    val selectedType = ImInt(prevType)
                    ImGui.combo("##type", selectedType, types)
                    if (selectedType.get() != prevType) {
                        EditBuildingStateChange(it, it.building!!.capacity, types[selectedType.get()])
                    } else {
                        null
                    }
                }
                .finish()
        )

        registerMenu(
            InspectorMenuBuilder<Road>("Road")
                .withItem("ID") { it.id }
                .withItem("Start junction ID") { it.startIntersection.id }
                .withItem("End junction ID") { it.endIntersection.id }
                .withCustomItem("Left lane count") {
                    val leftLaneCnt = ImInt(it.leftLane)
                    if (ImGui.inputInt("##left", leftLaneCnt)) {
                        leftLaneCnt.set(leftLaneCnt.get().coerceIn(Road.MIN_LANE_COUNT, Road.MAX_LANE_COUNT))
                    }
                    if (leftLaneCnt.get() != it.leftLane) {
                        EditRoadStateChange(it, leftLaneCnt.get(), it.rightLane)
                    } else {
                        null
                    }
                }
                .withCustomItem("Right lane count") {
                    val rightLaneCnt = ImInt(it.rightLane)
                    if (ImGui.inputInt("##right", rightLaneCnt)) {
                        rightLaneCnt.set(rightLaneCnt.get().coerceIn(Road.MIN_LANE_COUNT, Road.MAX_LANE_COUNT))
                    }
                    if (rightLaneCnt.get() != it.rightLane) {
                        EditRoadStateChange(it, it.leftLane, rightLaneCnt.get())
                    } else {
                        null
                    }
                }
                .finish()
        )

        registerMenu(
            InspectorMenuBuilder<Intersection>("Intersection")
                .withFilter { !it.isBuilding }
                .withItem("ID") { it.id }
                .withItem("Incoming roads IDs") { it.incomingRoads.map { it.id } }
                .withItem("Inner roads IDs") { it.intersectionRoads.values.map { it.id } }
                .withCustomItem("Has signals") { intersection ->
                    val hasSignalsChanged = ImGui.radioButton("##signals", intersection.hasSignals)
                    if (!hasSignalsChanged) {
                        return@withCustomItem null
                    }

                    val newSignals = HashMap<Road, Signal>()
                    if (!intersection.hasSignals) {
                        for (road in intersection.incomingRoads) {
                            newSignals[road] = Signal()
                        }
                    }

                    ReplaceIntersectionSignalsStateChange(intersection, newSignals)
                }
                .withCustomItem("Padding") { intersection ->
                    val padding = ImDouble(intersection.padding)
                    if (ImGui.inputDouble("##padding", padding)) {
                        padding.set(padding.get().coerceIn(0.0, Double.MAX_VALUE))
                    }
                    if (padding.get() != intersection.padding) {
                        IntersectionPaddingStateChange(intersection, padding.get())
                    } else {
                        null
                    }
                }
                .withCustomBlock { intersection ->
                    if (!intersection.hasSignals) {
                        return@withCustomBlock null
                    }
                    var stateChange: IStateChange? = null
                    for (road in intersection.incomingRoads) {
                        ImGui.pushID(road.id)
                        ImGui.tableNextRow()
                        ImGui.tableSetColumnIndex(0)
                        ImGui.text("Signal for road #${road.id}")
                        ImGui.tableSetColumnIndex(1)
                        val signal = intersection.signals[road]
                        if (signal == null) {
                            ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "ERROR: no signal found")
                            continue
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
                    stateChange
                }
                .finish()
        )
    }

    override fun getButtonName(): String = name

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false
        val groundPoint = getIntersectionWithGround(screenPos, camera) ?: return false

        if (handleIntersectionConfiguration(groundPoint)) {
            return true
        }

        selectedSubject = null
        lastClickPos = screenPos

        val intersection = findRoadIntersectionAt(layout, groundPoint)
        if (intersection != null) {
            selectedSubject = intersection
            drawIncomingConnections(intersection)
            return true
        } else {
            cleanUpIntersectionRoadsSettings()
        }
        val road = findRoad(layout, groundPoint)
        if (road != null) {
            selectedSubject = road
            return true
        }
        return false
    }

    private fun handleIntersectionConfiguration(groundPoint: Vec3): Boolean {
        if (incomingLanes.isEmpty()) {
            return false
        }

        if (selectedSubject is Intersection) {
            val intersection = selectedSubject as Intersection
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
                return true
            }
        } else {
            // one incoming and all outgoing lines are displayed

            val toRoad = findNearestObject(groundPoint, outgoingLanesSphere, Layout.LANE_WIDTH / 2) {
                it.transform.getTranslation(Vector3()).toVec3()
            }

            if (toRoad == null) {
                return false
            }

            val fromRoad = if (selectedFromRoad != null) {
                selectedFromRoad!!
            } else {
                logger.warn { "No selected from road when there are outgoing lanes?" }
                return true
            }

            if (selectedSubject is Intersection) {
                val intersection = selectedSubject as Intersection
                val connectingRoad = intersection.findConnectingRoad(fromRoad.road, fromRoad.lane, toRoad.road, toRoad.lane)
                if (connectingRoad == null) {
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
        val subject = selectedSubject ?: return null
        for ((klass, menu) in menus) {
            if (klass == subject::class) {
                @Suppress("UNCHECKED_CAST")
                val menu = (menu as InspectorMenu<Any>)
                if (menu.fits(subject)) {
                    val change = menu.runImgui(subject, lastClickPos)
                    lastClickPos = null
                    return change
                }
            }
        }
        return null
    }


    private fun drawIncomingConnections(intersection: Intersection) {
        incomingLanes.clear()
        for (road in intersection.incomingRoads) {
            for (i in 1..abs(road.getIncomingLaneCount(intersection))) {
                val sphereModel = createSphere(Color.GREEN, 1.0)
                val sphereInstance = ModelInstance(sphereModel)
                val position = road.getIntersectionPoint(intersection, (i.toDouble() - 0.5))
                position.y = 1.0
                sphereInstance.transform.setToTranslation(position.toGdxVec())
                incomingLanes.add(
                    LaneSphere(
                        intersection,
                        road,
                        i * road.getIncomingLaneCount(intersection).sign,
                        sphereInstance
                    )
                )
            }
        }
    }

    private fun drawOutgoingConnections(intersection: Intersection) {
        outgoingLanesSphere.clear()
        for (road in intersection.incomingRoads) {
            for (i in 1..abs(road.getOutgoingLaneCount(intersection))) {
                val realLaneNumber = i * road.getOutgoingLaneCount(intersection).sign

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
            cleanUpIntersectionRoadsSettings()
            selectedSubject = null
        }
    }

    private fun cleanUpIntersectionRoadsSettings() {
        incomingLanes.clear()
        outgoingLanesSphere.clear()
        selectedFromRoad = null
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

    private inline fun <reified T> registerMenu(menu: InspectorMenu<T>) {
        menus.add(T::class to menu)
    }
}
