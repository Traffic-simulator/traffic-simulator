package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import imgui.ImGui
import ru.nsu.trafficsimulator.editor.actions.LoadAction
import ru.nsu.trafficsimulator.editor.actions.SaveAction
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.tools.*
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.Layout

class Editor {
    companion object {
        var layout: Layout = Layout()
            set(value) {
                field = value
                onLayoutChange(true, true)
            }

        private lateinit var camera: Camera
        private var changes = ArrayList<IStateChange>()
        private var nextChange = 0

        private val actions = listOf(LoadAction(), SaveAction())
        private val inspectorTool = InspectorTool()
        private val tools = listOf(EditTool(), AddRoadTool(), AddBuildingTool(), DeleteRoadTool(), inspectorTool)

        private var viewOnly: Boolean = false

        private var currentTool = tools[0]

        private val spheres = mutableMapOf<Long, ModelInstance>()

        var onStructuralLayoutChange = mutableListOf<(Layout) -> Unit>()

        fun init(camera: Camera) {
            this.camera = camera
            onLayoutChange(generateLayoutMesh = true, reset = true)
        }

        fun runImgui() {
            if (!viewOnly) {
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
            }

            val change = currentTool.runImgui()
            if (!viewOnly && change != null) {
                appendChange(change)
            }
        }

        fun render(modelBatch: ModelBatch) {
            currentTool.render(modelBatch)

            for ((_, sphere) in spheres) {
                modelBatch.render(sphere)
            }
        }

        fun viewOnlyMode(viewOnly: Boolean) {
            this.viewOnly = viewOnly
            inspectorTool.viewOnly = viewOnly
            if (viewOnly && currentTool != inspectorTool) {
                currentTool = inspectorTool
                currentTool.init(layout, camera, true)
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
                    if (!viewOnly && change != null) {
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
            try {
                change.apply(layout)
            } catch (e: Exception) {
                logger.warn { "Can't apply change: $change due to: ${e.message}" }
            }
            onLayoutChange(change.isStructuralChange(), false)
        }

        private fun onLayoutChange(generateLayoutMesh: Boolean, reset: Boolean) {
            if (generateLayoutMesh) {
                for (observer in onStructuralLayoutChange) {
                    observer(layout)
                }
            }
            currentTool.init(layout, camera, reset)

            this.spheres.clear()
            val model = createSphere(Color.RED)
            for ((id, intersection) in layout.intersections) {
                spheres[id] = ModelInstance(model)
                spheres[id]!!.transform.setToTranslation(intersection.position.toVec3().toGdxVec())
            }
        }

        fun addRoadStats(stats: List<Pair<String, (id: Long) -> Any>>) {
            inspectorTool.addRoadStats(stats)
        }

        fun addIntersectionStats(stats: List<Pair<String, (id: Long) -> Any>>) {
            inspectorTool.addIntersectionStats(stats)
        }

        fun addVehicleStats(stats: List<Pair<String, (id: Int) -> Any>>) {
            inspectorTool.addVehicleStats(stats)
        }
    }
}
