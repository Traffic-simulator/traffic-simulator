package ru.nsu.trafficsimulator

import BackendAPI
import OpenDriveReader
import SpawnDetails
import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import vehicle.Direction
import kotlin.math.*

import net.mgsx.gltf.scene3d.scene.SceneSkybox
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.model_generation.ModelGenerator
import ru.nsu.trafficsimulator.serializer.Deserializer
import java.lang.Math.clamp
import kotlin.math.abs


class Main : ApplicationAdapter() {
    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var camera: PerspectiveCamera? = null
    private var environment: Environment? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset1: SceneAsset? = null
    private var tmpInputProcessor: InputProcessor? = null
    private var layoutModel: Model? = null

    private var carModel: Model? = null
    private var modelInstance1: ModelInstance? = null
    private var modelBatch: ModelBatch? = null

    private val back = BackendAPI()
    private val carInstances = mutableMapOf<Int, ModelInstance>()

    override fun create() {
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera?.position?.set(170f, 20f, -170f)
        camera?.lookAt(170f, 0.0f, -170.0f)
        camera?.near = 10.0f
        camera?.far = 1000f
        camera?.update()

        environment = Environment()
        environment!!.add(
            DirectionalShadowLight(1024, 1024, 1000.0f, 1000.0f, 0.1f, 1000.0f).set(
                0.9f,
                0.9f,
                0.9f,
                -0f,
                -1.0f,
                -0.2f
            )
        )

        sceneManager = SceneManager()
        sceneAsset1 = GLBLoader().load(Gdx.files.internal("racer.glb"))
        carModel = sceneAsset1?.scene?.model
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment

        val inputMultiplexer = InputMultiplexer()
        val camController = MyCameraController(camera!!)

        inputMultiplexer.addProcessor(createSphereEditorProcessor(camController))
        inputMultiplexer.addProcessor(camController)
        Gdx.input.inputProcessor = inputMultiplexer

        modelInstance1 = ModelInstance(carModel)
        modelBatch = ModelBatch()

        val odr = OpenDriveReader()
        val openDRIVE = odr.read("Town01.xodr")
        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
//        spawnDetails.add(Triple("20", "1", Direction.FORWARD))
//        spawnDetails.add(Triple("11", "1", Direction.FORWARD))
        spawnDetails.add(Triple("6", "-1", Direction.FORWARD))
//        spawnDetails.add(Triple("1", "1", Direction.BACKWARD))
//        spawnDetails.add(Triple("4", "1", Direction.BACKWARD))
//        spawnDetails.add(Triple("4", "-1", Direction.FORWARD))
//        spawnDetails.add(Triple("15", "-1", Direction.FORWARD))
//        spawnDetails.add(Triple("12", "-1", Direction.FORWARD))
//        spawnDetails.add(Triple("6", "1", Direction.FORWARD))
//        spawnDetails.add(Triple("13", "1", Direction.BACKWARD))

        back.init(openDRIVE, SpawnDetails(spawnDetails), 500)

//        layout = Deserializer().deserialize(OpenDriveReader().read("Town01.xodr"))
//        println(layout.roads)
//        for (i in layout.roads) {
//            println("${i.value.id} ${i.value.geometry}")
//        }
//        println(layout.intersectionRoads)

        /*val time = measureTime {
            layoutModel = createLayoutModel(layout_2)
        }
        println("Road layout model generation took $time")
        layoutScene = Scene(layoutModel)
        sceneManager?.addScene(layoutScene)*/

        // Add ground
        val modelBuilder = ModelBuilder()
        val groundMaterial = Material(PBRColorAttribute.createBaseColorFactor(Color(0.0f, 0.8f, 0.0f, 1.0f)))
        modelBuilder.begin()
        val meshPartBuilder = modelBuilder.part(
            "Ground",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            groundMaterial
        )
        BoxShapeBuilder.build(meshPartBuilder, 1000000.0f, 0.1f, 100000.0f)
        val ground = modelBuilder.end()
        sceneManager?.addScene(Scene(ground))

        sceneManager?.skyBox = SceneSkybox(
            Cubemap(
                Gdx.files.internal("skybox/right.png"),
                Gdx.files.internal("skybox/left.png"),
                Gdx.files.internal("skybox/top.png"),
                Gdx.files.internal("skybox/bottom.png"),
                Gdx.files.internal("skybox/front.png"),
                Gdx.files.internal("skybox/back.png"),
            )
        )
    }

    private var layout: Layout = Layout()
    private var layoutScene: Scene? = null

    private var addRoadStatus = false;
    private var editRoadStatus = false;
    private var currentEditRoadId: Long = 0;
    private var deleteRoadStatus = false;
    private var editStatus = true;

    private val lastTwoAddObjects = arrayOfNulls<Intersection>(2)
    private var sphereAddCounter = 0
    private val lastTwoDeleteIntersections = arrayOfNulls<Intersection>(2)
    private var intersectionDeleteCounter = 0

    private val spheres = mutableMapOf<Long, ModelInstance>()
    private val directionSpheres = mutableMapOf<Long, Pair<ModelInstance, ModelInstance>>()
    private val offsetDirectionSphere: Double = 25.0

    private var draggingSphere: ModelInstance? = null

    private fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
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
        val intersection = getIntersection(screenX, screenY)
        if (intersection != null) {
            val roadIntersection = findRoadIntersectionAt(intersection)
            if (roadIntersection != null) {
                lastTwoDeleteIntersections[intersectionDeleteCounter] = roadIntersection
                intersectionDeleteCounter += 1
                if (intersectionDeleteCounter == 2) {
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
            }
        }
    }

    private fun handleAddRoad(screenX: Int, screenY: Int) {
        val intersectionPoint = getIntersection(screenX, screenY)
        if (intersectionPoint != null) {
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
            if (sphereAddCounter == 2) {
                val spherePairModel = createDirectionPairSphere()
                val startInstance = ModelInstance(spherePairModel.first)
                val endInstance = ModelInstance(spherePairModel.second)

                if (lastTwoAddObjects[0] !== lastTwoAddObjects[1]) {
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
                sphereAddCounter = 0
                addRoadStatus = false
            }
        }
    }

    private fun updateLayout() {
        if (layoutScene != null) {
            sceneManager?.removeScene(layoutScene)
        }
        layoutModel = ModelGenerator.createLayoutModel(layout)
        layoutScene = Scene(layoutModel)
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

    override fun render() {
        if (tmpInputProcessor != null) {
            Gdx.input.inputProcessor = tmpInputProcessor
            tmpInputProcessor = null
        }
        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()
        run {
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
                println(layout)
            }
            ImGui.end()
        }
        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        // Получаем данные о положении машинок
        val vehicleData = back.getNextFrame(0.01)
//      println("Vehicle data: $vehicleData")

        // Обновляем позиции машинок
        updateCars(vehicleData)

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        sceneManager?.update(Gdx.graphics.deltaTime)
        sceneManager?.render()

        modelBatch?.begin(camera)
        for (car in carInstances.values) {
            modelBatch?.render(car, environment)
        }
        if (editStatus) {
            for ((_, sphere) in spheres) {
                modelBatch?.render(sphere)
            }
            if (editRoadStatus) {
                modelBatch?.render(directionSpheres[currentEditRoadId]!!.first)
                modelBatch?.render(directionSpheres[currentEditRoadId]!!.second)
            }
        }
        modelBatch?.end()

        imGuiGl3.renderDrawData(ImGui.getDrawData())
    }

    override fun dispose() {
        image?.dispose()
        carModel?.dispose()
        imGuiGl3.shutdown()
        imGuiGlfw.shutdown()
        ImGui.destroyContext()
    }

    override fun resize(width: Int, height: Int) {
        sceneManager?.updateViewport(width.toFloat(), height.toFloat())
    }

    private fun updateCars(vehicleData: List<ISimulation.VehicleDTO>) {

        for (vehicle in vehicleData) {
            val vehicleId = vehicle.id
            val carRoad = vehicle.road

            // Если машина не добавлена, создаем новую ModelInstance
            if (!carInstances.containsKey(vehicleId)) {
                carInstances[vehicleId] = ModelInstance(carModel)
            }
            val carInstance = carInstances[vehicleId]!!

            val spline = Deserializer.planeViewToSpline(carRoad.planView)
            val pointOnSpline = clamp(if (vehicle.direction == Direction.BACKWARD) { spline.length - vehicle.distance } else { vehicle.distance }, 0.0, spline.length)
            val pos = spline.getPoint(pointOnSpline)
            val dir = spline.getDirection(pointOnSpline).normalized() * if (vehicle.direction == Direction.BACKWARD) { -1.0 } else { 1.0 }
            val right = Vec3(dir.x, 0.0, dir.y).cross(Vec3(0.0, 1.0, 0.0)).normalized()
            val angle = - acos(dir.x) * sign(dir.y)
            val laneOffset = (abs(vehicle.laneId) - 0.5)
            carInstance.transform.setToRotationRad(Vector3(0.0f, 1.0f, 0.0f), angle.toFloat())
                .setTranslation((pos.x + laneOffset * right.x * ModelGenerator.laneWidth).toFloat(), 1.0f, (pos.y + laneOffset * right.z * ModelGenerator.laneWidth).toFloat())
        }

        // Удаление машин, которых больше нет в vehicleData
        val vehicleIds = vehicleData.map { it.id }.toSet()
        val removedKeys = carInstances.keys - vehicleIds
        for (key in removedKeys) {
            carInstances.remove(key)
        }
    }
}

class MyCameraController(camera: Camera) : CameraInputController(camera) {
    var camaraEnabled = true

    override fun keyDown(keycode: Int): Boolean {
        if (camaraEnabled) {
            super.keyDown(keycode)
        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (camaraEnabled) {
            super.touchDragged(screenX, screenY, pointer)
        }
        return false
    }
}
