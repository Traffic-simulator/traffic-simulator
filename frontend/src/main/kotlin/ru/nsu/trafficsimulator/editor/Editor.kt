package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import imgui.ImGui
import imgui.type.ImInt
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import ru.nsu.trafficsimulator.editor.actions.LoadAction
import ru.nsu.trafficsimulator.editor.actions.SaveAction
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.tools.*
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.graphics.ModelGenerator
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import kotlin.math.sign

class Editor {
    companion object {
        var layout: Layout = Layout()
            set(value) {
                field = value
                onLayoutChange(true, true)
            }
        private var layoutScene: Scene? = null
        var sceneManager: SceneManager? = null
        var camera: Camera? = null
        private var changes = ArrayList<IStateChange>()
        private var nextChange = 0

        private val actions = listOf(LoadAction(), SaveAction())
        private val tools = listOf(EditTool(), AddRoadTool(), AddBuildingTool(), DeleteRoadTool(), InspectorTool())

        private var currentTool = tools[0]

        private val spheres = mutableMapOf<Long, ModelInstance>()

        private var trafficLightModel: Model = GLBLoader().load(Gdx.files.internal("models/traffic_light.glb")).scene!!.model
        private var trafficLights = mutableMapOf<Pair<Road, Boolean>, Scene>()

        fun init(camera: Camera, sceneManager: SceneManager) {
            this.camera = camera
            this.sceneManager = sceneManager
            onLayoutChange(true, true)
        }

        fun runImgui() {
            ImGui.begin("Editor")
            ImGui.labelText("##actions", "Available Actions:")
            for (action in actions) {
                if (action.runImgui()) {
                    if (action.runAction(layout)) {
                        onLayoutChange(action.isStructuralAction(), true)
                    }
                }
            }
            if (ImGui.button("Undo")) {
                if (nextChange > 0) {
                    nextChange--;
                    changes[nextChange].revert(layout)
                    layout.intersections.values.forEach { it.recalculateIntersectionRoads() }
                    onLayoutChange(changes[nextChange].isStructuralChange(), false)
                }
            }
            if (ImGui.button("Redo")) {
                if (nextChange < changes.size) {
                    changes[nextChange].apply(layout)
                    nextChange++
                    layout.intersections.values.forEach { it.recalculateIntersectionRoads() }
                    onLayoutChange(changes[nextChange - 1].isStructuralChange(), false)
                }
            }

            ImGui.separator()
            ImGui.labelText("##tools", "Available Tools:")
            for (tool in tools) {
                if (ImGui.selectable(tool.getButtonName(), currentTool == tool)) {
                    currentTool = tool
                    onLayoutChange(false, true)
                }
            }
            ImGui.end()

            val change = currentTool.runImgui()
            if (change != null) {
                appendChange(change)
            }
        }


        fun render(modelBatch: ModelBatch) {
            currentTool.render(modelBatch)

            for ((_, sphere) in spheres) {
                modelBatch.render(sphere)
            }
        }

        fun createSphereEditorProcessor(): InputProcessor {
            return object : InputAdapter() {
                var grabInput: Boolean = false
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    grabInput = currentTool.handleDown(Vec2(screenX.toDouble(), screenY.toDouble()), button)
                    return grabInput
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    val change = currentTool.handleUp(Vec2(screenX.toDouble(), screenY.toDouble()), button)
                    if (change != null) {
                        appendChange(change)
                    }
                    val prevGrabInput = grabInput
                    grabInput = false
                    return prevGrabInput
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    currentTool.handleDrag(Vec2(screenX.toDouble(), screenY.toDouble()))
                    return grabInput
                }
            }
        }

        private fun appendChange(change: IStateChange) {
            while (changes.size > nextChange) {
                changes.removeLast()
            }
            changes.add(change)
            nextChange++
            change.apply(layout)
            onLayoutChange(change.isStructuralChange(), false)
        }

        private fun onLayoutChange(generateLayoutMesh: Boolean, reset: Boolean) {
            if (generateLayoutMesh) {
                updateLayout()
            }
            currentTool.init(layout, camera!!, reset)

            this.spheres.clear()
            val model = createSphere(Color.RED)
            for ((id, intersection) in layout.intersections) {
                spheres[id] = ModelInstance(model)
                spheres[id]!!.transform.setToTranslation(intersection.position.toVec3().toGdxVec())
            }
        }

        private fun updateLayout() {
            logger.info("Updating layout, roads: ${layout.roads.size}, intersections: ${layout.intersections.size}")
            if (layoutScene != null) {
                sceneManager?.removeScene(layoutScene)
            }
            layoutScene = Scene(ModelGenerator.createLayoutModel(layout))
            sceneManager?.addScene(layoutScene)

            // Update traffic lights
            val visited = mutableSetOf<Pair<Road, Boolean>>()
            for ((id, intersection) in layout.intersections) {
                if (!intersection.hasSignals) {
                    continue
                }
                for (road in intersection.incomingRoads) {
                    val isStart = road.startIntersection == intersection
                    val distFromStart = if (isStart) { 0.0 } else { road.length }
                    val key = road to isStart
                    visited.add(key)
                    if (!trafficLights.containsKey(key)) {
                        val trafficLight = Scene(trafficLightModel)
                        trafficLights[key] = trafficLight
                        sceneManager?.addScene(trafficLights[key])
                    }
                    val scene = trafficLights[key]!!
                    val centerPos = road.getPoint(distFromStart)
                    val roadDirection = road.getDirection(distFromStart)
                    val laneId = if (isStart) { -road.leftLane } else { road.rightLane }.toDouble()
                    val offsetLen = if (isStart) {
                        road.leftLane
                    } else {
                        road.rightLane
                    }.toDouble() * LANE_WIDTH + LANE_WIDTH / 2
                    val offset = roadDirection.cross(Vec3.UP).normalized() * laneId.sign * offsetLen
                    val position = centerPos + offset
                    val targetPos = centerPos + roadDirection * laneId.sign
                    val newTransform = Matrix4()
                        .setTranslation(position.toGdxVec())
                        .rotateTowardTarget(targetPos.toGdxVec(), Vec3.UP.toGdxVec())
                    scene.modelInstance.transform.set(newTransform)
                }
            }

            val toRemove = mutableSetOf<Pair<Road, Boolean>>()
            for ((key, scene) in trafficLights) {
                if (!visited.contains(key)) {
                    toRemove.add(key)
                    sceneManager?.removeScene(scene)
                }
            }
            for (key in toRemove) {
                trafficLights.remove(key)
            }
        }
    }
}
