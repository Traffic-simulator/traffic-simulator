package ru.nsu.trafficsimulator;

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.Vector3
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Vec3


class Main : ApplicationAdapter() {
    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var color = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var camera: PerspectiveCamera? = null
    private var model: Model? = null
    private var environment: Environment? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset: SceneAsset? = null
    private var tmpInputProcessor: InputProcessor? = null
    private var layout: Layout = Layout()


    companion object {
        fun createLayoutMesh(layout: Layout): Model {
            val roadHeight = 0.5
            val laneWidth = 2.0
            val intersectionPadding = 10.0f

            val modelBuilder = ModelBuilder()
            var meshPartBuilder: MeshPartBuilder
            modelBuilder.begin()
            for (road in layout.getRoads()) {
                val node = modelBuilder.node()
                val pos = (road.startIntersection.position + road.endIntersection.position) / 2.0
                val dir = road.startIntersection.position - road.endIntersection.position
                val halfLen = dir.length() / 2.0 - intersectionPadding
                if (halfLen < 0)
                    continue
                val halfDir = dir.normalized() * halfLen
                val right = halfDir.cross(Vec3(0.0, 1.0, 0.0)).normalized() * 2.0 * laneWidth
                node.translation.set(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                meshPartBuilder = modelBuilder.part("road${road.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
                BoxShapeBuilder.build(
                    meshPartBuilder,
                    Vector3((-halfDir.x - right.x).toFloat(), roadHeight.toFloat(), (-halfDir.z - right.z).toFloat()),
                    Vector3((-halfDir.x + right.x).toFloat(), roadHeight.toFloat(), (-halfDir.z + right.z).toFloat()),
                    Vector3((halfDir.x - right.x).toFloat(), roadHeight.toFloat(), (halfDir.z - right.z).toFloat()),
                    Vector3((halfDir.x + right.x).toFloat(), roadHeight.toFloat(), (halfDir.z + right.z).toFloat()),
                    Vector3((-halfDir.x - right.x).toFloat(), 0.0f, (-halfDir.z - right.z).toFloat()),
                    Vector3((-halfDir.x + right.x).toFloat(), 0.0f, (-halfDir.z + right.z).toFloat()),
                    Vector3((halfDir.x - right.x).toFloat(), 0.0f, (halfDir.z - right.z).toFloat()),
                    Vector3((halfDir.x + right.x).toFloat(), 0.0f, (halfDir.z + right.z).toFloat()),
                )
            }

            return modelBuilder.end()
        }
    }

    override fun create() {
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera?.position?.set(10f, 10f, 10f)
        camera?.lookAt(0.0f, 0.0f, 0.0f)
        camera?.near = 0.5f
        camera?.far = 1000f
        camera?.update()

        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0.8f, -0.2f))

        sceneManager = SceneManager()
        sceneAsset = GLBLoader().load(Gdx.files.internal("car.glb"))
        // sceneManager?.addScene(Scene(sceneAsset?.scene))
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment

        val camController = CameraInputController(camera)
        camController.translateUnits = 100.0f
        Gdx.input.inputProcessor = camController

        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(100.0, 0.0, 0.0))
        layout.addRoad(Vec3(100.0, 0.0, -100.0), Vec3(100.0, 0.0, 0.0))
        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 100.0))
        layout.addRoad(Vec3(100.0, 0.0, 100.0), Vec3(0.0, 0.0, 100.0))
        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(100.0, 0.0, 100.0))
        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(-100.0, 0.0, -100.0))
        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(-100.0, 0.0, 100.0))
        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(100.0, 0.0, -100.0))
        val layoutModel = createLayoutMesh(layout)
        val layoutScene = Scene(layoutModel)
        sceneManager?.addScene(layoutScene)
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
            ImGui.begin("Hello world!")
            ImGui.text("Choose color for background:")
            ImGui.colorEdit3("background", color)
            ImGui.end()
        }
        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height);
        Gdx.gl.glClearColor(color[0], color[1], color[2], 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        sceneManager?.update(Gdx.graphics.deltaTime)
        sceneManager?.render()

        imGuiGl3.renderDrawData(ImGui.getDrawData())
    }

    override fun dispose() {
        image?.dispose()
        model?.dispose()
        imGuiGl3.shutdown()
        imGuiGlfw.shutdown()
        ImGui.destroyContext()
    }

    override fun resize(width: Int, height: Int) {
        sceneManager?.updateViewport(width.toFloat(), height.toFloat());
    }
}
