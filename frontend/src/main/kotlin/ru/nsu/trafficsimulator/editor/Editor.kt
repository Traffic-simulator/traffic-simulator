package ru.nsu.trafficsimulator.editor

import OpenDriveWriter
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import imgui.ImGui
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import ru.nsu.trafficsimulator.MyCameraController
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.model_generation.ModelGenerator

class Editor {
    companion object {
        private var layout: Layout = Layout()
        private var layoutScene: Scene? = null
        var sceneManager: SceneManager? = null
        var camera: Camera? = null

        private val selectedIntersections = arrayOfNulls<Intersection>(2)
        private var selectedIntersectionCount = 0
        private val offsetDirectionSphere: Double = 25.0

        private val curveCoeff: Double = 4.0

        private var draggingDirectionSphere: ModelInstance? = null
        private var draggingDirectionIsStart: Boolean? = null

        private var addRoadStatus = false
        private var editRoadStatus = false
        private var editRoadSelected = false
        private var currentEditRoadId: Long? = null
        private var deleteRoadStatus = false
        private val tools = listOf(InspectTool(), AddRoadTool(), DeleteRoadTool())
        private var currentTool = tools[0]

        private val directionSpheres = mutableMapOf<Long, Pair<ModelInstance, ModelInstance>>()

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
//            if (ImGui.button("Add road")) {
//                addRoadStatus = true
//                deleteRoadStatus = false
//                selectedIntersectionCount = 0
//            }
//            if (ImGui.button("Edit road")) {
//                editRoadStatus = !editRoadStatus
//                editRoadSelected = false
//                currentEditRoadId = null
//                draggingDirectionIsStart = null
//                selectedIntersectionCount = 0
//            }
//            if (ImGui.button("Delete road")) {
//                deleteRoadStatus = true
//                addRoadStatus = false
//                selectedIntersectionCount = 0
//            }
            ImGui.end()
        }

        fun render(modelBatch: ModelBatch?) {
            currentTool.render(modelBatch)
//            if (editRoadStatus && currentEditRoadId != null) {
//                modelBatch?.render(directionSpheres[currentEditRoadId]!!.first)
//                modelBatch?.render(directionSpheres[currentEditRoadId]!!.second)
//            }
        }

        fun getLayout(): Layout {
            return layout
        }

        fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
            return object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    camController.camaraEnabled = !currentTool.handleDown(Vec2(screenX.toDouble(), screenY.toDouble()), button)
//                    if (button == Input.Buttons.LEFT) {
//                        if (addRoadStatus) {
//
//                        }
//                        if (deleteRoadStatus) {
//                            handleDeleteRoad(screenX, screenY)
//                        }
//                        if (editRoadStatus) {
//                            handleEditRoad(screenX, screenY, camController)
//                        }
//                    }
                    return false
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (currentTool.handleUp(Vec2(screenX.toDouble(), screenY.toDouble()), button)) {
                        updateLayout()
                    }
                    camController.camaraEnabled = (button == Input.Buttons.LEFT)
                    return false
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    currentTool.handleDrag(Vec2(screenX.toDouble(), screenY.toDouble()))
//                    if (draggingDirectionSphere != null && editRoadSelected) {
//                        val intersection = getIntersection(screenX, screenY)
//                        if (intersection != null) {
//                            draggingDirectionSphere!!.transform.setToTranslation(intersection)
//                            val editedRoad = layout.roads[currentEditRoadId]
//                            if (editedRoad != null) {
//                                val direction = Vector3()
//                                draggingDirectionSphere?.transform?.getTranslation(direction)
//                                val directionVec = Vec3(direction.x, direction.y, direction.z)
//                                if (draggingDirectionIsStart == true) {
//                                    val t = editedRoad.startIntersection?.position?.minus(directionVec)
//                                        ?.times(curveCoeff)
//                                    editedRoad.redirectRoad(
//                                        editedRoad.startIntersection!!,
//                                        editedRoad.startIntersection!!.position + t!!
//                                    )
//                                }
//                                if (draggingDirectionIsStart == false) {
//                                    val t = editedRoad.endIntersection?.position?.minus(directionVec)?.times(
//                                        curveCoeff
//                                    )
//                                    editedRoad.redirectRoad(
//                                        editedRoad.endIntersection!!,
//                                        editedRoad.endIntersection!!.position + t!!
//                                    )
//                                }
//                            }
//                        }
//                    }
                    return false
                }
            }
        }

//        private fun handleEditRoad(screenX: Int, screenY: Int, camController: MyCameraController) {
//            if (editRoadSelected) {
//                val intersection = getIntersection(screenX, screenY)
//                if (intersection != null) {
//                    draggingDirectionSphere = findDirectionSphere(intersection)
//                    if (draggingDirectionSphere != null) {
//                        camController.camaraEnabled = false
//                    }
//                }
//            } else {
//                val intersection = getIntersection(screenX, screenY) ?: return
//                val road = findRoad(intersection) ?: return
//                currentEditRoadId = road.id
//                editRoadSelected = true
//            }
//        }
//
//        private fun handleDeleteRoad(screenX: Int, screenY: Int) {
//            deleteRoadStatus = false
//            val intersection = getIntersection(screenX, screenY) ?: return
//            val road = findRoad(intersection) ?: return
//            layout.deleteRoad(road)
//            if (road.startIntersection!!.incomingRoads.size == 0) {
//                spheres.remove(road.startIntersection!!.id)
//            }
//            if (road.endIntersection!!.incomingRoads.size == 0) {
//                spheres.remove(road.endIntersection!!.id)
//            }
//            updateLayout()
//        }
//
//

        private fun updateLayout() {
            if (layoutScene != null) {
                sceneManager?.removeScene(layoutScene)
            }
            layoutScene = Scene(ModelGenerator.createLayoutModel(layout))
            sceneManager?.addScene(layoutScene)
        }

        private fun findDirectionSphere(intersection: Vector3): ModelInstance? {
            if (directionSpheres[currentEditRoadId]!!.first.transform.getTranslation(Vector3())
                    .dst(intersection) < 5.0f
            ) {
                draggingDirectionIsStart = true
                return directionSpheres[currentEditRoadId]?.first
            }
            if (directionSpheres[currentEditRoadId]!!.second.transform.getTranslation(Vector3())
                    .dst(intersection) < 5.0f
            ) {
                draggingDirectionIsStart = false
                return directionSpheres[currentEditRoadId]?.second
            }
            draggingDirectionIsStart = null
            return null
        }

        private fun createDirectionPairSphere(): Pair<Model, Model> {
            val modelBuilder = ModelBuilder()

            val startMaterial = Material(ColorAttribute.createDiffuse(Color.BLUE))
            val start = modelBuilder.createSphere(
                5.0f,
                5.0f,
                5.0f,
                10,
                10,
                startMaterial,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            )
            val endMaterial = Material(ColorAttribute.createDiffuse(Color.SKY))
            val end = modelBuilder.createSphere(
                5.0f,
                5.0f,
                5.0f,
                10,
                10,
                endMaterial,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            )
            return Pair(start, end)
        }
    }
}
