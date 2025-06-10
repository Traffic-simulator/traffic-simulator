package ru.nsu.trafficsimulator

import BackendAPI
import ISimulation
import OpenDriveReader
import OpenDriveWriter
import Waypoint
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import mu.KotlinLogging
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import ru.nsu.trafficsimulator.editor.Editor
import ru.nsu.trafficsimulator.editor.logger
import ru.nsu.trafficsimulator.graphics.CustomShaderProvider
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.serializer.serializeLayout
import signals.SignalState
import vehicle.Direction
import java.lang.Math.clamp
import kotlin.math.*


class Main : ApplicationAdapter() {
    private enum class ApplicationState {
        Editor,
        Simulator,
    }

    private data class SimulationState(val backend: ISimulation, var isPaused: Boolean = false, var speed: Double = 1.0)

    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var camera: PerspectiveCamera? = null
    private var environment: Environment? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset1: SceneAsset? = null
    private var tmpInputProcessor: InputProcessor? = null

    private var carModel: Model? = null
    private var modelInstance1: ModelInstance? = null
    private var modelBatch: ModelBatch? = null

    private var buildingModel: Model? = null
    private var buildingScenes = mutableListOf<Scene>()

    private var simState: SimulationState = SimulationState(BackendAPI())
    private val carInstances = mutableMapOf<Int, Scene>()
    private var state = ApplicationState.Editor
    private var editorInputProcess: InputProcessor? = null
    private val inputMultiplexer = InputMultiplexer()

    private val logger = KotlinLogging.logger("FRONTEND")

    override fun create() {
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

        camera = PerspectiveCamera(66f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
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
        sceneAsset1 = GLBLoader().load(Gdx.files.internal("models/racer_big.glb"))
        val buildingAsset = GLBLoader().load(Gdx.files.internal("models/building.glb"))
        buildingModel = buildingAsset?.scene?.model
        carModel = sceneAsset1?.scene?.model
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment
        sceneManager?.setShaderProvider(CustomShaderProvider("shaders/pbr.vs.glsl", "shaders/pbr.fs.glsl"))

        editorInputProcess = Editor.createSphereEditorProcessor()
        inputMultiplexer.addProcessor(editorInputProcess)

        val camController = CameraInputController(camera!!)
        camController.scrollFactor = -0.2f
        camController.rotateAngle = 180f
        camController.translateUnits = 130f
        camController.target = camera!!.position

        inputMultiplexer.addProcessor(camController)

        Gdx.input.inputProcessor = inputMultiplexer

        modelInstance1 = ModelInstance(carModel)
        modelBatch = ModelBatch()

        // Add ground
        val modelBuilder = ModelBuilder()
        val groundMaterial = Material(PBRColorAttribute.createBaseColorFactor(Color(0.0f, 0.8f, 0.0f, 1.0f)))
        print("Ground: ")
        for (attribute in groundMaterial) {
            print("$attribute, ")
        }
        println()
        modelBuilder.begin()
        val meshPartBuilder = modelBuilder.part(
            "Ground",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            groundMaterial
        )
        BoxShapeBuilder.build(meshPartBuilder, 1000.0f, 0.1f, 1000.0f)
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
        Editor.init(camera!!, sceneManager!!)
//        val dto = OpenDriveReader().read("self_made_town_01.xodr")
//        Editor.layout = Deserializer.deserialize(dto)
    }

    fun initializeSimulation(layout: Layout) {
        val spawnDetails = ArrayList<Waypoint>()
        val despawnDetails = ArrayList<Waypoint>()
//        spawnDetails.add(Waypoint("58", "1", Direction.BACKWARD))
//        spawnDetails.add(Waypoint("31", "1", Direction.BACKWARD))
//        spawnDetails.add(Waypoint("31", "1", Direction.BACKWARD))
//        spawnDetails.add(Waypoint("1", "1", Direction.BACKWARD))
//        spawnDetails.add(Waypoint("10", "1", Direction.BACKWARD))
//
//        despawnDetails.add(Waypoint("58", "-1", Direction.FORWARD))
//        despawnDetails.add(Waypoint("31", "-1", Direction.FORWARD))
//        despawnDetails.add(Waypoint("31", "-1", Direction.FORWARD))
//        despawnDetails.add(Waypoint("1", "-1", Direction.FORWARD))
//        despawnDetails.add(Waypoint("10", "-1", Direction.FORWARD))

//        spawnDetails.add(Waypoint("0", "1", Direction.BACKWARD))
//        despawnDetails.add(Waypoint("0", "-1", Direction.FORWARD))

        val dto = serializeLayout(layout)
        OpenDriveWriter().write(dto, "export.xodr")
//        val dto = OpenDriveReader().read("self_made_town_01.xodr")
//        Editor.layout = Deserializer.deserialize(dto)
        simState.backend.init(dto, spawnDetails, despawnDetails, 500)
    }

    private fun initializeBuildings(layout: Layout) {
        buildingScenes.forEach { sceneManager?.removeScene(it) }
        buildingScenes.clear()

        layout.intersections.values.forEach { intersection ->
            if (intersection.isBuilding) {
                val buildingScene = Scene(buildingModel)

                val center = intersection.position.toVec3()
                buildingScene.modelInstance.transform
                    .setToTranslation(center.toGdxVec())
                    .scale(0.7f, 0.7f, 0.7f)

                sceneManager?.addScene(buildingScene)
                buildingScenes.add(buildingScene)
            }
        }
    }

    val FRAMETIME = 0.01 // It's 1 / FPS, duration of one frame in seconds

    override fun render() {
        val frameStartTime = System.nanoTime()
        if (state == ApplicationState.Simulator && !simState.isPaused) {
            simState.backend.updateSimulation(FRAMETIME * simState.speed)
            updateCars(simState.backend.getVehicles())
            updateSignals(simState.backend.getSignalStates())
        }

        if (tmpInputProcessor != null) {
            Gdx.input.inputProcessor = tmpInputProcessor
            tmpInputProcessor = null
        }
        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()

        renderSimulationMenu()
        initializeBuildings(Editor.layout)

        if (state == ApplicationState.Editor) {
            Editor.runImgui()
        }
        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        sceneManager?.update(Gdx.graphics.deltaTime)
        sceneManager?.render()

        if (state == ApplicationState.Editor) {
            modelBatch?.let {
                it.begin(camera)
                Editor.render(it)
                it.end()
            }
        }

        imGuiGl3.renderDrawData(ImGui.getDrawData())

        val currentTime = System.nanoTime()
        val iterationsMillis = (currentTime - frameStartTime) / 1_000_000.0
//        logger.debug("Render iteration took $iterationsMillis ms, will spin for ${(FRAMETIME * 1000 - iterationsMillis).toFloat()} ms")

        // Spinning for the rest of frame time
        while ((System.nanoTime() - frameStartTime) / 1_000_000_000.0 < FRAMETIME) {
        }
    }

    private fun renderSimulationMenu() {
        ImGui.begin("Simulation Controls")
        val startStopText = if (state == ApplicationState.Editor) {
            "Start"
        } else {
            "Stop"
        }
        if (ImGui.button(startStopText)) {
            state = when (state) {
                ApplicationState.Editor -> ApplicationState.Simulator
                ApplicationState.Simulator -> ApplicationState.Editor
            }
            if (state == ApplicationState.Editor) {
                inputMultiplexer.addProcessor(0, editorInputProcess)
            } else {
                inputMultiplexer.removeProcessor(editorInputProcess)
                initializeSimulation(Editor.layout)
            }
        }
        if (state == ApplicationState.Simulator) {
            if (ImGui.button("||")) {
                simState.isPaused = !simState.isPaused
            }
            ImGui.sameLine()
            if (ImGui.button(">")) {
                simState.speed = 1.0
            }
            ImGui.sameLine()
            if (ImGui.button(">>")) {
                simState.speed = 2.0
            }
            ImGui.sameLine()
            if (ImGui.button(">>>")) {
                simState.speed = 5.0
            }
        }
        ImGui.end()
    }

    override fun dispose() {
        image?.dispose()
        carModel?.dispose()
        buildingModel?.dispose()
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
                carInstances[vehicleId] = Scene(carModel)
                sceneManager?.addScene(carInstances[vehicleId])
            }
            val carInstance = carInstances[vehicleId]!!

            val spline = Deserializer.planeViewToSpline(carRoad.planView)
            val pointOnSpline = clamp(
                if (vehicle.direction == Direction.BACKWARD) {
                    spline.length - vehicle.distance
                } else {
                    vehicle.distance
                }, 0.0, spline.length
            )
            val pos = spline.getPoint(pointOnSpline)
            val dir = spline.getDirection(pointOnSpline).normalized() * if (vehicle.direction == Direction.BACKWARD) {
                -1.0
            } else {
                1.0
            }
            val right = dir.toVec3().cross(Vec3.UP).normalized()
            val angle = acos(dir.x) * sign(dir.y)
            val laneOffset = (abs(vehicle.laneId) - 0.5)
            val finalTranslation = pos.toVec3() + right * laneOffset * LANE_WIDTH + Vec3.UP
            carInstance
                .modelInstance
                .transform
                .setToRotationRad(Vec3.UP.toGdxVec(), angle.toFloat())
                .setTranslation(finalTranslation.toGdxVec())
        }

        // Удаление машин, которых больше нет в vehicleData
        val vehicleIds = vehicleData.map { it.id }.toSet()
        val removedKeys = carInstances.keys - vehicleIds
        for (key in removedKeys) {
            sceneManager?.removeScene(carInstances[key])
            carInstances.remove(key)
        }
    }

    private fun updateSignals(signalData: List<ISimulation.SignalDTO>) {
        println(signalData)
        for ((key, signalScene) in Editor.trafficLights) {
            val signalModel = signalScene.modelInstance
            for (material in signalModel.materials) {
                material.remove(PBRColorAttribute.Emissive)
                material.remove(PBRFloatAttribute.EmissiveIntensity)
            }
        }
        for (signal in signalData) {
            val roadId = signal.road.id.toLong()
            if (!Editor.layout.roads.containsKey(roadId)) {
                logger.warn { "Failed to find road with id from SignalDTO" }
                continue
            }
            val road = Editor.layout.roads[roadId]!!

            val (intersection, isStart) = if (abs(signal.distFromLaneStart - road.length) < 1e-3) {
                road.endIntersection to false
            } else if (abs(signal.distFromLaneStart) < 1e-3) {
                road.startIntersection to true
            } else {
                logger.warn { "distFromLaneStart was ${signal.distFromLaneStart} which is inconclusive" }
                continue
            }

            val signalModel = Editor.trafficLights[road to isStart]!!.modelInstance

            val lightNames = when (signal.state) {
                SignalState.RED -> setOf("light-red")
                SignalState.GREEN -> setOf("color-green")
                SignalState.YELLOW -> setOf("light-yellow")
                SignalState.RED_YELLOW -> setOf("light-yellow", "light-red")
            }

            for (lightName in lightNames) {
                for (material in signalModel.materials) {
                    if (!lightNames.contains(material.id)) {
                        continue
                    }
                    var color: Color? = null
                    for (attribute in material) {
                        if (attribute.type == PBRColorAttribute.BaseColorFactor) {
                            color = (attribute as PBRColorAttribute).color
                        }
                    }
                    material.set(PBRColorAttribute.createEmissive(color))
                    material.set(PBRFloatAttribute.createEmissiveIntensity(1.0f))
                }
            }
        }
    }
}
