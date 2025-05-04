package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import imgui.ImGui
import imgui.ImVec2
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import ru.nsu.trafficsimulator.editor.actions.LoadAction
import ru.nsu.trafficsimulator.editor.actions.SaveAction
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.tools.AddRoadTool
import ru.nsu.trafficsimulator.editor.tools.DeleteRoadTool
import ru.nsu.trafficsimulator.editor.tools.InspectTool
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.graphics.ModelGenerator

class Editor {
    companion object {
        var layout: Layout = Layout()
            set(value) {
                field = value
                onLayoutChange(true)
            }
        private var layoutScene: Scene? = null
        var sceneManager: SceneManager? = null
        var camera: Camera? = null
        private var changes = ArrayList<IStateChange>()
        private var nextChange = 0

        private val actions = listOf(LoadAction(), SaveAction())
        private val tools = listOf(InspectTool(), AddRoadTool(), DeleteRoadTool())
        private var currentTool = tools[0]

        private val spheres = mutableMapOf<Long, ModelInstance>()

        fun init(camera: Camera, sceneManager: SceneManager) {
            this.camera = camera
            this.sceneManager = sceneManager
            onLayoutChange(true)
        }

        fun runImgui() {
            ImGui.begin("Editor")
            ImGui.labelText("##actions", "Available Actions:")
            for (action in actions) {
                if (action.runImgui()) {
                    if (action.runAction(layout)) {
                        onLayoutChange(true)
                    }
                }
            }
            if (ImGui.button("Undo")) {
                if (nextChange > 0) {
                    nextChange--;
                    changes[nextChange].revert(layout)
                    layout.intersections.values.forEach { it.recalculateIntersectionRoads() }
                    onLayoutChange(false)
                }
            }
            if (ImGui.button("Redo")) {
                if (nextChange < changes.size) {
                    changes[nextChange].apply(layout)
                    nextChange++
                    layout.intersections.values.forEach { it.recalculateIntersectionRoads() }
                    onLayoutChange(false)
                }
            }

            ImGui.separator()
            ImGui.labelText("##tools", "Available Tools:")
            for (tool in tools) {
                if (ImGui.selectable(tool.getButtonName(), currentTool == tool)) {
                    currentTool = tool
                    onLayoutChange(false)
                }
            }
            ImGui.end()
        }

        fun render(modelBatch: ModelBatch?) {
            currentTool.render(modelBatch)

            for ((_, sphere) in spheres) {
                modelBatch?.render(sphere)
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
                        while (changes.size > nextChange) {
                            changes.removeLast()
                        }
                        changes.add(change)
                        nextChange++
                        change.apply(layout)
                        onLayoutChange(false)
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

        private fun onLayoutChange(reset: Boolean) {
            updateLayout()
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
        }
    }
}
