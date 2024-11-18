package ru.nsu.trafficsimulator;

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Vector3
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import kotlin.math.cos
import kotlin.math.sin


class Main : ApplicationAdapter() {
    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var camera: PerspectiveCamera? = null
    private var environment: Environment? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset1: SceneAsset? = null
    private var sceneAsset2: SceneAsset? = null
    private var tmpInputProcessor: InputProcessor? = null

    private var carX = floatArrayOf(0.0f)
    private var carY = floatArrayOf(0.0f)
    private var carZ = floatArrayOf(0.0f)
    private var elapsedTime = 0f
    private var rotationAngle = 1f

    private var model1: Model? = null
    private var modelInstance1: ModelInstance? = null
    private var modelBatch1: ModelBatch? = null

    private var model2: Model? = null
    private var modelInstance2: ModelInstance? = null
    private var modelBatch2: ModelBatch? = null

    override fun create() {
        image = Texture("libgdx.png")
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera?.position?.set(10f, 10f, 10f)
        camera?.lookAt(0.0f, 0.0f, 0.0f)
        camera?.near = 1f
        camera?.far = 300f
        camera?.update()

        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0.8f, -0.2f))

        sceneManager = SceneManager()
        sceneAsset1 = GLBLoader().load(Gdx.files.internal("cop.glb"))
        sceneAsset2 = GLBLoader().load(Gdx.files.internal("racer.glb"))
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment

        modelInstance1 = ModelInstance(sceneAsset1?.scene?.model)
        modelBatch1 = ModelBatch()
        modelInstance2 = ModelInstance(sceneAsset2?.scene?.model)
        modelBatch2 = ModelBatch()

        val camController = CameraInputController(camera);
        Gdx.input.inputProcessor = camController;
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
            ImGui.begin("Position")
            ImGui.text("Choose x value")
            ImGui.sliderFloat("x", carX, -20.0f, 20.0f)
            ImGui.text("Choose y value")
            ImGui.sliderFloat("y", carY, -20.0f, 20.0f)
            ImGui.text("Choose z value")
            ImGui.sliderFloat("z", carZ, -20.0f, 20.0f)
            ImGui.end()
        }
        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        elapsedTime += Gdx.graphics.deltaTime
        modelInstance1?.transform?.setTranslation(5 * sin(elapsedTime), carY[0], 5 * cos(elapsedTime))
        modelInstance1?.transform?.rotate(Vector3(0f, 1f, 0f), rotationAngle)
        modelInstance2?.transform?.setTranslation(5 * sin(elapsedTime + 1), carY[0], 5 * cos(elapsedTime + 1))
        modelInstance2?.transform?.rotate(Vector3(0f, 1f, 0f), rotationAngle)

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height);
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        sceneManager?.update(Gdx.graphics.deltaTime)
        sceneManager?.render()

        modelBatch1?.begin(camera)
        modelBatch1?.render(modelInstance1, environment)
        modelBatch1?.end()

        modelBatch2?.begin(camera)
        modelBatch2?.render(modelInstance2, environment)
        modelBatch2?.end()

        imGuiGl3.renderDrawData(ImGui.getDrawData())
    }

    override fun dispose() {
        image?.dispose()
        model1?.dispose()
        model2?.dispose()
        imGuiGl3.shutdown()
        imGuiGlfw.shutdown()
        ImGui.destroyContext()
    }

    override fun resize(width: Int, height: Int) {
        sceneManager?.updateViewport(width.toFloat(), height.toFloat());
    }
}
