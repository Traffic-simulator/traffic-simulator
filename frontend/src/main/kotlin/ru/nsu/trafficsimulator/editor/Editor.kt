package ru.nsu.trafficsimulator.editor

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
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Vec3
import ru.nsu.trafficsimulator.model_generation.ModelGenerator

class Editor {
    companion object {
        private var layout: Layout = Layout()
        private var layoutScene: Scene? = null
        var sceneManager: SceneManager? = null
        var camera: Camera? = null

        private val lastTwoAddObjects = arrayOfNulls<Intersection>(2)
        private var sphereAddCounter = 0
        private val lastTwoDeleteIntersections = arrayOfNulls<Intersection>(2)
        private var intersectionDeleteCounter = 0
        private val offsetDirectionSphere: Double = 25.0
        private var draggingSphere: ModelInstance? = null

        private var addRoadStatus = false
        private var editRoadStatus = false
        private var currentEditRoadId: Long = 0
        private var deleteRoadStatus = false
        private var editStatus = true

        private val spheres = mutableMapOf<Long, ModelInstance>()
        private val directionSpheres = mutableMapOf<Long, Pair<ModelInstance, ModelInstance>>()

        fun runImgui() {
            ImGui.begin("Editor")
            if (ImGui.button("Add road")) {
                addRoadStatus = true
                deleteRoadStatus = false
            }
            if (ImGui.button("Edit road")) {
                editRoadStatus = !editRoadStatus
            }
            if (ImGui.button("Delete road")) {
                deleteRoadStatus = true
                addRoadStatus = false
            }
            if (ImGui.button("Edit mode")) {
                editStatus = !editStatus
            }
            ImGui.end()
        }

        fun render(modelBatch: ModelBatch?) {
            if (editStatus) {
                for ((_, sphere) in spheres) {
                    modelBatch?.render(sphere)
                }
                if (editRoadStatus) {
                    modelBatch?.render(directionSpheres[currentEditRoadId]!!.first)
                    modelBatch?.render(directionSpheres[currentEditRoadId]!!.second)
                }
            }
        }

        fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
            return object : InputAdapter() {
                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (button == Input.Buttons.LEFT && editStatus) {
                        if (!addRoadStatus && !deleteRoadStatus && !editRoadStatus) {
                            val intersection = getIntersection(screenX, screenY)
                            if (intersection != null) {
                                draggingSphere = findSphereAt(intersection)?.second
                                if (draggingSphere != null) {
                                    camController.camaraEnabled = false
                                }
                            }
                        }
                        if (addRoadStatus) {
                            handleAddRoad(screenX, screenY)
                        }
                        if (deleteRoadStatus) {
                            handleDeleteRoad(screenX, screenY)
                        }
                    }
                    return false
                }

                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    if (button == Input.Buttons.LEFT && editStatus) {
                        draggingSphere = null
                        camController.camaraEnabled = true
                        updateLayout()
                    }
                    return false
                }

                override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                    if (draggingSphere != null && editStatus) {
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
                                    roadIntersection,
                                    Vec3(intersection.x.toDouble(), intersection.y.toDouble(), intersection.z.toDouble())
                                )
                            }
                            draggingSphere!!.transform.setToTranslation(intersection)
                        }
                    }
                    return false
                }
            }
        }

        private fun handleDeleteRoad(screenX: Int, screenY: Int) {
            val intersection = getIntersection(screenX, screenY) ?: return
            val roadIntersection = findRoadIntersectionAt(intersection) ?: return
            lastTwoDeleteIntersections[intersectionDeleteCounter] = roadIntersection
            intersectionDeleteCounter += 1
            if (intersectionDeleteCounter != 2)
                return

            val road = findRoad(lastTwoDeleteIntersections[0]!!, lastTwoDeleteIntersections[1]!!)
            if (road != null) {
                layout.deleteRoad(road)
            }
            val deleteStatus1 = lastTwoDeleteIntersections[0]!!.incomingRoads.size == 0
            val deleteStatus2 = lastTwoDeleteIntersections[1]!!.incomingRoads.size == 0
            if (deleteStatus1) {
                spheres.remove(findSphereAt(lastTwoDeleteIntersections[0]!!.position.toGdxVec())!!.first)
            }
            if (deleteStatus2) {
                spheres.remove(findSphereAt(lastTwoDeleteIntersections[1]!!.position.toGdxVec())!!.first)
            }
            updateLayout()
            intersectionDeleteCounter = 0
            deleteRoadStatus = false
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
            lastTwoAddObjects[sphereAddCounter] = roadIntersection

            sphereAddCounter += 1
            if (sphereAddCounter != 2)
                return
            sphereAddCounter = 0
            addRoadStatus = false
            if (lastTwoAddObjects[0] == lastTwoAddObjects[1])
                return

            val startDirection = Vec3(
                lastTwoAddObjects[0]!!.position.x + offsetDirectionSphere,
                lastTwoAddObjects[0]!!.position.y,
                lastTwoAddObjects[0]!!.position.z + offsetDirectionSphere
            )
            val endDirection = Vec3(
                lastTwoAddObjects[1]!!.position.x + offsetDirectionSphere,
                lastTwoAddObjects[1]!!.position.y,
                lastTwoAddObjects[1]!!.position.z + offsetDirectionSphere
            )
            val spherePairModel = createDirectionPairSphere()
            val startInstance = ModelInstance(spherePairModel.first)
            val endInstance = ModelInstance(spherePairModel.second)
            startInstance.transform.setToTranslation(startDirection.toGdxVec())
            endInstance.transform.setToTranslation(endDirection.toGdxVec())
            val road = layout.addRoad(
                lastTwoAddObjects[0]!!,
                startDirection,
                lastTwoAddObjects[1]!!,
                endDirection
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

        private fun findRoad(intersection1: Intersection, intersection2: Intersection): Road? {
            for (road in layout.roads.values) {
                if (road.startIntersection === intersection1 && road.endIntersection === intersection2) {
                    return road
                }
                if (road.startIntersection === intersection2 && road.endIntersection === intersection1) {
                    return road
                }
            }
            return null
        }

        private fun findRoadId(intersection1: Intersection?, intersection2: Intersection?): Long? {
            for (key in layout.roads.keys) {
                if (layout.roads[key]?.startIntersection === intersection1 && layout.roads[key]?.endIntersection === intersection2) {
                    return key
                }
                if (layout.roads[key]?.startIntersection === intersection2 && layout.roads[key]?.endIntersection === intersection1) {
                    return key
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
                5.0f, 5.0f, 5.0f, 10,
                10, startMaterial, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            )
            val endMaterial = Material(ColorAttribute.createDiffuse(Color.SKY))
            val end = modelBuilder.createSphere(
                5.0f, 5.0f, 5.0f, 10,
                10, endMaterial, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            )
            return Pair(start, end)
        }

        private fun createSphere(): Model? {
            val modelBuilder = ModelBuilder()
            val material = Material(ColorAttribute.createDiffuse(Color.RED))
            val sphere = modelBuilder.createSphere(
                5.0f, 5.0f, 5.0f, 10,
                10, material, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            )
            return sphere
        }
    }
}
