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
import ru.nsu.trafficsimulator.serializer.serializeLayout

class Editor {
    companion object {
        private var layout: Layout = Layout()
        private var layoutScene: Scene? = null
        var sceneManager: SceneManager? = null
        var camera: Camera? = null

        private val selectedIntersections = arrayOfNulls<Intersection>(2)
        private var selectedIntersectionCount = 0
        private val offsetDirectionSphere: Double = 25.0
        private val roadIntersectionThreshold: Double = 5.0
        private val curveCoeff: Double = 4.0
        private var draggingSphere: ModelInstance? = null
        private var draggingDirectionSphere: ModelInstance? = null
        private var draggingDirectionIsStart: Boolean? = null

        private var addRoadStatus = false
        private var editRoadStatus = false
        private var editRoadSelected = false
        private var currentEditRoadId: Long? = null
        private var deleteRoadStatus = false
        private var editStatus = true

        private val spheres = mutableMapOf<Long, ModelInstance>()
        private val directionSpheres = mutableMapOf<Long, Pair<ModelInstance, ModelInstance>>()

        fun runImgui() {
            ImGui.begin("Editor")
            if (ImGui.button("Add road")) {
                addRoadStatus = true
                deleteRoadStatus = false
                selectedIntersectionCount = 0
            }
            if (ImGui.button("Edit road")) {
                editRoadStatus = !editRoadStatus
                editRoadSelected = false
                currentEditRoadId = null
                draggingDirectionIsStart = null
                selectedIntersectionCount = 0
            }
            if (ImGui.button("Delete road")) {
                deleteRoadStatus = true
                addRoadStatus = false
                selectedIntersectionCount = 0
            }
            if (ImGui.button("Edit mode")) {
                editStatus = !editStatus
                selectedIntersectionCount = 0
                println(layout)
                val writer = OpenDriveWriter()
                writer.write(serializeLayout(layout),"testSerializer")
            }
            ImGui.end()
        }

        fun render(modelBatch: ModelBatch?) {
            if (editStatus) {
                for ((_, sphere) in spheres) {
                    modelBatch?.render(sphere)
                }
                if (editRoadStatus && currentEditRoadId != null) {
                    modelBatch?.render(directionSpheres[currentEditRoadId]!!.first)
                    modelBatch?.render(directionSpheres[currentEditRoadId]!!.second)
                }
            }
        }

        fun getLayout(): Layout {
            return layout
        }

        fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
            return object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    println("click")
                    if (button == Input.Buttons.LEFT && editStatus) {
                        if (editRoadSelected) {
                            val intersection = getIntersection(screenX, screenY)
                            if (intersection != null) {
                                draggingDirectionSphere = findDirectionSphere(intersection)
                                if (draggingDirectionSphere != null) {
                                    camController.camaraEnabled = false
                                }
                            }
                        } else {
                            if (!addRoadStatus && !deleteRoadStatus && !editRoadStatus) {
                                handleMoveIntersection(screenX, screenY, camController)
                            }
                            if (addRoadStatus) {
                                handleAddRoad(screenX, screenY)
                            }
                            if (deleteRoadStatus) {
                                handleDeleteRoad(screenX, screenY)
                            }
                            if (editRoadStatus) {
                                handleEditRoad(screenX, screenY, camController)
                            }
                        }
                    }
                    return false
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (button == Input.Buttons.LEFT) {
                        if (editStatus) {
                            draggingSphere = null
                            camController.camaraEnabled = true
                            updateLayout()
                            if (editRoadSelected) {
                                draggingDirectionSphere = null
                            }
                        }
                    }
                    return false
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (draggingSphere != null && editStatus && !editRoadStatus) {
                        val intersection = getIntersection(screenX, screenY)
                        if (intersection != null) {
                            var roadIntersection: Intersection? = null
                            for ((id, sphere) in spheres) {
                                if (sphere == draggingSphere) {
                                    roadIntersection = layout.intersections[id]
                                }
                            }
                            if (roadIntersection != null) {
                                layout.moveIntersection(
                                    roadIntersection, Vec3(
                                        intersection.x.toDouble(), intersection.y.toDouble(), intersection.z.toDouble()
                                    )
                                )
                                draggingSphere!!.transform.setToTranslation(intersection)
                            }
                        }
                    }
                    if (draggingDirectionSphere != null && editStatus && editRoadSelected) {
                        val intersection = getIntersection(screenX, screenY)
                        if (intersection != null) {
                            draggingDirectionSphere!!.transform.setToTranslation(intersection)
                            val editedRoad = layout.roads[currentEditRoadId]
                            if (editedRoad != null) {
                                val direction = Vector3()
                                draggingDirectionSphere?.transform?.getTranslation(direction)
                                val directionVec = Vec3(direction.x, direction.y, direction.z)
                                if (draggingDirectionIsStart == true) {
                                    val t = editedRoad.startIntersection?.position?.minus(directionVec)
                                        ?.times(curveCoeff)
                                    editedRoad.redirectRoad(
                                        editedRoad.startIntersection!!,
                                        editedRoad.startIntersection!!.position + t!!
                                    )
                                }
                                if (draggingDirectionIsStart == false) {
                                    val t = editedRoad.endIntersection?.position?.minus(directionVec)?.times(
                                        curveCoeff
                                    )
                                    editedRoad.redirectRoad(
                                        editedRoad.endIntersection!!,
                                        editedRoad.endIntersection!!.position + t!!
                                    )
                                }
                            }
                        }
                    }
                    return false
                }
            }
        }

        private fun handleMoveIntersection(screenX: Int, screenY: Int, camController: MyCameraController) {
            val intersection = getIntersection(screenX, screenY)
            if (intersection != null) {
                draggingSphere = findSphereAt(intersection)?.second
                if (draggingSphere != null) {
                    camController.camaraEnabled = false
                }
            }
        }

        private fun handleEditRoad(screenX: Int, screenY: Int, camController: MyCameraController) {
            val intersection = getIntersection(screenX, screenY) ?: return
            val road = findRoad(intersection) ?: return
            currentEditRoadId = road.id
            editRoadSelected = true
        }

        private fun handleDeleteRoad(screenX: Int, screenY: Int) {
            deleteRoadStatus = false
            val intersection = getIntersection(screenX, screenY) ?: return
            val road = findRoad(intersection) ?: return
            layout.deleteRoad(road)
            if (road.startIntersection!!.incomingRoads.size == 0) {
                spheres.remove(road.startIntersection!!.id)
            }
            if (road.endIntersection!!.incomingRoads.size == 0) {
                spheres.remove(road.endIntersection!!.id)
            }
            updateLayout()
        }

        private fun handleAddRoad(screenX: Int, screenY: Int) {
            val intersectionPoint = getIntersection(screenX, screenY) ?: return

            var roadIntersection = findRoadIntersectionAt(intersectionPoint)
            if (roadIntersection == null) {
                roadIntersection = layout.addIntersection(Vec3(intersectionPoint))
                val sphereModel = createSphere()
                val sphereInstance = ModelInstance(sphereModel)
                sphereInstance.transform.setToTranslation(intersectionPoint)
                spheres[roadIntersection.id] = sphereInstance
            }
            selectedIntersections[selectedIntersectionCount] = roadIntersection

            selectedIntersectionCount += 1
            if (selectedIntersectionCount != 2) return
            selectedIntersectionCount = 0
            addRoadStatus = false
            if (selectedIntersections[0] == selectedIntersections[1]) return


            val startDirection = Vec3(
                selectedIntersections[0]!!.position.x + offsetDirectionSphere,
                selectedIntersections[0]!!.position.y,
                selectedIntersections[0]!!.position.z + offsetDirectionSphere
            )
            val endDirection = Vec3(
                selectedIntersections[1]!!.position.x + offsetDirectionSphere,
                selectedIntersections[1]!!.position.y,
                selectedIntersections[1]!!.position.z + offsetDirectionSphere
            )
            val spherePairModel = createDirectionPairSphere()
            val startInstance = ModelInstance(spherePairModel.first)
            val endInstance = ModelInstance(spherePairModel.second)

            startInstance.transform.setToTranslation(startDirection.toGdxVec())
            endInstance.transform.setToTranslation(endDirection.toGdxVec())


            val road = layout.addRoad(
                selectedIntersections[0]!!, startDirection, selectedIntersections[1]!!, endDirection
            )
            directionSpheres[road.id] = startInstance to endInstance
            updateLayout()
        }

        private fun updateLayout() {
            if (layoutScene != null) {
                sceneManager?.removeScene(layoutScene)
            }
            layoutScene = Scene(ModelGenerator.createLayoutModel(layout))
            sceneManager?.addScene(layoutScene)
        }

        private fun findRoad(intersection: Vector3): Road? {
            var minDistance = Double.MAX_VALUE
            for (road in layout.roads.values) {
                val point = Vec2(intersection.x.toDouble(), intersection.z.toDouble())
                val distance = road.geometry.closestPoint(point).distance(point)
                if (distance < minDistance) {
                    minDistance = distance
                    if (minDistance < roadIntersectionThreshold) {
                        return road
                    }
                }
            }
            return null
        }

        private fun findRoadIntersectionAt(intersection: Vector3): Intersection? {
            for ((id, sphere) in spheres) {
                if (sphere.transform.getTranslation(Vector3()).dst(intersection) < 5.0f) {
                    return layout.intersections[id]
                }
            }
            return null
        }

        private fun findSphereAt(intersection: Vector3): Pair<Long, ModelInstance>? {
            for ((id, sphere) in spheres) {
                if (sphere.transform.getTranslation(Vector3()).dst(intersection) < 5.0f) {
                    return id to sphere
                }
            }
            return null
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

        private fun getIntersection(screenX: Int, screenY: Int): Vector3? {
            val ray = camera!!.getPickRay(screenX.toFloat(), screenY.toFloat())
            val intersection = Vector3()
            val plane = Plane(Vector3(0f, 1f, 0f), 0f)
            if (Intersector.intersectRayPlane(ray, plane, intersection)) {
                return intersection
            }
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

        private fun createSphere(): Model? {
            val modelBuilder = ModelBuilder()
            val material = Material(ColorAttribute.createDiffuse(Color.RED))
            val sphere = modelBuilder.createSphere(
                5.0f,
                5.0f,
                5.0f,
                10,
                10,
                material,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            )
            return sphere
        }
    }
}
