package ru.nsu.trafficsimulator

import ru.nsu.trafficsimulator.backend.BackendAPI
import ISimulation
import OpenDriveReader
import OpenDriveWriter
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.MathUtils.clamp
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImInt
import mu.KotlinLogging
import org.lwjgl.glfw.GLFW
import ru.nsu.trafficsimulator.editor.Editor
import ru.nsu.trafficsimulator.graphics.Visualizer
import ru.nsu.trafficsimulator.math.transformVehicles
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.serializeLayout
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger("FRONTEND")

class Main : ApplicationAdapter() {
    private enum class ApplicationState {
        Editor,
        Simulator,
    }

    private data class SimulationState(
        val backend: ISimulation,
        var isPaused: Boolean = false,
        var speed: Long = 1,
        var startTime: LocalTime = LocalTime.ofSecondOfDay(60 * 60 * 8),
        var currentTime: LocalTime = startTime,
        var region: Int? = null,
    )

    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()

    private var tmpInputProcessor: InputProcessor? = null

    private var state = ApplicationState.Editor
    private var simState: SimulationState = SimulationState(BackendAPI())

    private var editorInputProcess: InputProcessor? = null
    private val inputMultiplexer = InputMultiplexer()

    private lateinit var visualizer: Visualizer

    // It's 1 / FPS, duration of one frame in milliseconds
    private val FRAMETIME = ISimulation.Constants.SIMULATION_FRAME_MILLIS

    override fun create() {
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()
        GLFW.glfwSwapInterval(0)
        ImGui.getStyle().scaleAllSizes(2.0f)

        visualizer = Visualizer(Editor.layout)

        editorInputProcess = Editor.createSphereEditorProcessor()
        inputMultiplexer.addProcessor(editorInputProcess)

        val camera = visualizer.getCamera()
        val camController = CameraInputController(camera)
        camController.scrollFactor = -0.2f
        camController.rotateAngle = 180f
        camController.translateUnits = 130f
        camController.target = camera.position

        inputMultiplexer.addProcessor(camController)

        Gdx.input.inputProcessor = inputMultiplexer

        Editor.init(camera)
        Editor.onStructuralLayoutChange.add { visualizer.updateLayout(it) }

        Editor.addRoadStats(simState.backend.getRoadStats())
        Editor.addIntersectionStats(simState.backend.getIntersectionStats())
        Editor.addVehicleStats(simState.backend.getVehicleStats())
    }

    fun initializeSimulation(layout: Layout) {
        val dto = serializeLayout(layout)
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH.mm.ss")
        val formattedDateTime = currentDateTime.format(formatter)

        // val dto2 = simState.backend.gatherSimulationStats(dto, 500)
        OpenDriveWriter().write(dto, "export_$formattedDateTime.xodr")
        // val dto2 = simState.backend.gatherSimulationStats(OpenDriveReader().read("sausages4.xodr"), 500)
        simState.backend.init(dto, ISimulation.DrivingSide.RIGHT, simState.region, simState.startTime, 500)
    }

    override fun render() {
        // Update
        val frameStartTime = System.nanoTime()
        if (state == ApplicationState.Simulator && !simState.isPaused) {
            simState.backend.updateSimulation(FRAMETIME * simState.speed)
            val vehicles = transformVehicles(simState.backend.getVehicles())
            visualizer.updateCars(vehicles)
            Editor.updateVehicles(vehicles)
            visualizer.updateSignals(simState.backend.getSignalStates())
            visualizer.updateHeatmap(simState.backend.getSegments())

            simState.currentTime = simState.backend.getSimulationTime()
        }
        visualizer.updateSelectedItem(Editor.getSelectedItem())

        if (tmpInputProcessor != null) {
            Gdx.input.inputProcessor = tmpInputProcessor
            tmpInputProcessor = null
        }

        // Prepare UI
        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()

        renderSimulationMenu()

        Editor.runImgui()

        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        // Actual Render
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        visualizer.render(Gdx.graphics.deltaTime)

        if (state == ApplicationState.Editor) {
            val modelBatch = visualizer.getModelBatch()
            val camera = visualizer.getCamera()
            modelBatch.begin(camera)
            Editor.render(modelBatch)
            modelBatch.end()
        }

        imGuiGl3.renderDrawData(ImGui.getDrawData())

        val currentTime = System.nanoTime()
        val iterationsMillis = (currentTime - frameStartTime) / 1_000_000.0
//        logger.debug("Render iteration took $iterationsMillis ms, will spin for ${(FRAMETIME - iterationsMillis).toFloat()} ms")

        // Spinning for the rest of frame time
        while ((System.nanoTime() - frameStartTime) / 1_000_000.0 < FRAMETIME.toDouble()) {
        }
    }

    private fun renderSimulationMenu() {
        ImGui.begin("Simulation Controls")
        val startStopText = if (state == ApplicationState.Editor) {
            "Start"
        } else {
            "Stop"
        }

        if (state == ApplicationState.Editor) {
            ImGui.pushItemWidth(100.0f)
            ImGui.labelText("##TimeLabel", "Time: ")
            ImGui.sameLine()
            val hours = ImInt(simState.startTime.hour)
            ImGui.inputInt("##hours", hours)
            ImGui.sameLine()
            val minutes = ImInt(simState.startTime.minute)
            ImGui.inputInt("##minutes", minutes)
            ImGui.sameLine()
            val seconds = ImInt(simState.startTime.second)
            ImGui.inputInt("##seconds", seconds)
            ImGui.popItemWidth()

            simState.startTime = simState.startTime
                .withHour(clamp(hours.get(), 0, 23))
                .withMinute(clamp(minutes.get(), 0, 59))
                .withSecond(clamp(seconds.get(), 0, 59))
        } else {
            ImGui.labelText("##TimeLabel", "Time: ${simState.currentTime}")
        }

        if (ImGui.button(startStopText)) {
            state = when (state) {
                ApplicationState.Editor -> ApplicationState.Simulator
                ApplicationState.Simulator -> ApplicationState.Editor
            }
            if (state == ApplicationState.Editor) {
                visualizer.cleanup()
                Editor.viewOnlyMode(false)
            } else {
                Editor.viewOnlyMode(true)
                initializeSimulation(Editor.layout)
            }
        }
        val isRegion = simState.region != null
        ImGui.sameLine()
        if (ImGui.radioButton("Simulate region", isRegion)) {
            simState.region = if (isRegion) { null } else { 1 }
        }

        if (simState.region != null) {
            val regionId = ImInt(simState.region!!)
            ImGui.sameLine()
            ImGui.pushItemWidth(100.0f)
            if (ImGui.inputInt("Region id", regionId)) {
                simState.region = regionId.get().coerceIn(1, 4)
            }
            ImGui.popItemWidth()
        }

        if (state == ApplicationState.Simulator) {
            val pauseLabel = if (simState.isPaused) {
                "|>"
            } else {
                "||"
            }
            if (ImGui.button(pauseLabel)) {
                simState.isPaused = !simState.isPaused
            }
            ImGui.sameLine()
            if (ImGui.button("x1")) {
                simState.speed = 1
            }
            ImGui.sameLine()
            if (ImGui.button("x5")) {
                simState.speed = 5
            }
            ImGui.sameLine()
            if (ImGui.button("x10")) {
                simState.speed = 10
            }
            ImGui.sameLine()
            if (ImGui.button("x60")) {
                simState.speed = 60
            }
            ImGui.sameLine()
            if (ImGui.button("x480")) {
                simState.speed = 480
            }
            if (ImGui.radioButton("Display Heatmap", visualizer.heatmapMode)) {
                visualizer.heatmapMode = !visualizer.heatmapMode
            }
        }
        ImGui.end()
    }

    override fun dispose() {
        image?.dispose()
        visualizer.dispose()
        imGuiGl3.shutdown()
        imGuiGlfw.shutdown()
        ImGui.destroyContext()
    }

    override fun resize(width: Int, height: Int) {
        visualizer.onResize(width, height)
    }
}
