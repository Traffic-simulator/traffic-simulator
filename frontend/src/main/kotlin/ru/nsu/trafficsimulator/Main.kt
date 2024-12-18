package ru.nsu.trafficsimulator

import BackendAPI
import OpenDriveReader
import SpawnDetails
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import ktx.math.unaryMinus
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import opendrive.TRoadPlanViewGeometry
import vehicle.Direction
import kotlin.math.*

import net.mgsx.gltf.scene3d.scene.SceneSkybox
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.model_generation.ModelGenerator
import ru.nsu.trafficsimulator.serializer.Deserializer
import java.lang.Math.clamp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.time.measureTime


class Main : ApplicationAdapter() {
    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var camera: PerspectiveCamera? = null
    private var environment: Environment? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset1: SceneAsset? = null
    private var tmpInputProcessor: InputProcessor? = null
    private var layout: Layout = Layout()
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
        environment!!.add(DirectionalShadowLight(1024, 1024, 1000.0f, 1000.0f, 0.1f, 1000.0f).set(0.9f, 0.9f, 0.9f, -0f, -1.0f, -0.2f))

        sceneManager = SceneManager()
        sceneAsset1 = GLBLoader().load(Gdx.files.internal("racer.glb"))
        carModel = sceneAsset1?.scene?.model
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment

        val camController = CameraInputController(camera)
        camController.translateUnits = 100.0f
        Gdx.input.inputProcessor = camController

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

        layout = Deserializer.deserialize(OpenDriveReader().read("Town01.xodr"))

        val time = measureTime {
            layoutModel = ModelGenerator.createLayoutModel(layout)
        }
        println("Road layout model generation took $time")
        val layoutScene = Scene(layoutModel)
        sceneManager?.addScene(layoutScene)

        // Add ground
        val modelBuilder = ModelBuilder()
        val groundMaterial = Material(PBRColorAttribute.createBaseColorFactor(Color(0.0f, 0.8f, 0.0f, 1.0f)))
        modelBuilder.begin()
        val meshPartBuilder = modelBuilder.part("Ground", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), groundMaterial)
        BoxShapeBuilder.build(meshPartBuilder, 1000000.0f, 0.1f, 100000.0f)
        val ground = modelBuilder.end()
        sceneManager?.addScene(Scene(ground))

        sceneManager?.skyBox = SceneSkybox(Cubemap(
            Gdx.files.internal("skybox/right.png"),
            Gdx.files.internal("skybox/left.png"),
            Gdx.files.internal("skybox/top.png"),
            Gdx.files.internal("skybox/bottom.png"),
            Gdx.files.internal("skybox/front.png"),
            Gdx.files.internal("skybox/back.png"),
        ))
    }

    override fun render() {
        if (tmpInputProcessor != null) {
            Gdx.input.inputProcessor = tmpInputProcessor
            tmpInputProcessor = null
        }
        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()
        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        // Получаем данные о положении машинок
        val vehicleData = back.getNextFrame(0.01)
//        println("Vehicle data: $vehicleData")

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

    private var lastPos = Vec2(0.0, 0.0)
    // Храним предыдущую геометрию для каждой машины
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
//            println("${lastPos.x} -> ${pos.x} (${pos.x - lastPos.x})")
            lastPos = pos
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
