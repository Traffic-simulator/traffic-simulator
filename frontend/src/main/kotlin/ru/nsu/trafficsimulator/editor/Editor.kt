package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import imgui.ImGui
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import ru.nsu.trafficsimulator.MyCameraController
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.model_generation.ModelGenerator

class Editor {
    companion object {
        var layout: Layout = Layout()
        private var layoutScene: Scene? = null
        var sceneManager: SceneManager? = null
        var camera: Camera? = null

        private val tools = listOf(InspectTool(), AddRoadTool(), DeleteRoadTool())
        private var currentTool = tools[0]

        fun init(camera: Camera, sceneManager: SceneManager) {
            this.camera = camera
            this.sceneManager = sceneManager
            currentTool.init(layout, camera)
        }

        fun runImgui() {
            ImGui.begin("Editor")
            for (tool in tools) {
                if (ImGui.button(tool.getButtonName())) {
                    currentTool = tool
                    currentTool.init(layout, camera!!)
                }
            }
            ImGui.end()
        }

        fun render(modelBatch: ModelBatch?) {
            currentTool.render(modelBatch)

        }

        fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
            return object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    camController.camaraEnabled = !currentTool.handleDown(Vec2(screenX.toDouble(), screenY.toDouble()), button)
                    return false
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    val change = currentTool.handleUp(Vec2(screenX.toDouble(), screenY.toDouble()), button)
                    if (change != null) {
                        change.apply(layout)
                        updateLayout()
                    }
                    camController.camaraEnabled = (button == Input.Buttons.LEFT)
                    return false
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    currentTool.handleDrag(Vec2(screenX.toDouble(), screenY.toDouble()))

                    return false
                }
            }
        }

         fun updateLayout() {
            if (layoutScene != null) {
                sceneManager?.removeScene(layoutScene)
            }
            layoutScene = Scene(ModelGenerator.createLayoutModel(layout))
            sceneManager?.addScene(layoutScene)
        }
    }
}
