package ru.nsu.trafficsimulator

import BackendAPI
import ISimulation
import OpenDriveWriter
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import mu.KotlinLogging
import ru.nsu.trafficsimulator.editor.Editor
import ru.nsu.trafficsimulator.graphics.Visualizer
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.serializeLayout

val logger = KotlinLogging.logger("FRONTEND")

class Main : ApplicationAdapter() {
    private enum class ApplicationState {
        Editor,
        Simulator,
    }

    private data class SimulationState(val backend: ISimulation, var isPaused: Boolean = false, var speed: Double = 1.0)

    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()

    private var tmpInputProcessor: InputProcessor? = null

    private var state = ApplicationState.Editor
    private var simState: SimulationState = SimulationState(BackendAPI())

    private var editorInputProcess: InputProcessor? = null
    private val inputMultiplexer = InputMultiplexer()

    private lateinit var visualizer: Visualizer

    private val FRAMETIME = 0.01 // It's 1 / FPS, duration of one frame in seconds

    override fun create() {
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

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
    }

    fun initializeSimulation(layout: Layout) {
        val dto = serializeLayout(layout)
        OpenDriveWriter().write(dto, "export.xodr")
//        val dto = OpenDriveReader().read("self_made_town_01.xodr")
//        Editor.layout = Deserializer.deserialize(dto)
        simState.backend.init(dto,500)
    }

    override fun render() {
        // Update
        val frameStartTime = System.nanoTime()
        if (state == ApplicationState.Simulator && !simState.isPaused) {
            simState.backend.updateSimulation(FRAMETIME * simState.speed)
            visualizer.updateCars(simState.backend.getVehicles())
            visualizer.updateSignals(simState.backend.getSignalStates())
        }

        if (tmpInputProcessor != null) {
            Gdx.input.inputProcessor = tmpInputProcessor
            tmpInputProcessor = null
        }

        // Prepare UI
        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()

        renderSimulationMenu()

        if (state == ApplicationState.Editor) {
            Editor.runImgui()
        }
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
            val pauseLabel = if (simState.isPaused) { "|>" } else { "||" }
            if (ImGui.button(pauseLabel)) {
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
        visualizer.dispose()
        imGuiGl3.shutdown()
        imGuiGlfw.shutdown()
        ImGui.destroyContext()
    }

    override fun resize(width: Int, height: Int) {
        visualizer.onResize(width, height)
    }
}
