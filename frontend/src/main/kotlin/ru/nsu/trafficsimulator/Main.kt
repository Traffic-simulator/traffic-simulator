package ru.nsu.trafficsimulator;

import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.model.data.ModelData
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import ktx.math.unaryMinus
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import ru.nsu.trafficsimulator.model.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.measureTime


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

class Main : ApplicationAdapter() {
    private var camera: PerspectiveCamera? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()

    private lateinit var model: Model
    private lateinit var modelInstance: ModelInstance
    private lateinit var modelBatch: ModelBatch

    private val plane = Plane(Vector3(0f, 1f, 0f), 0f)

    private var status = false;
    private var layout: Layout = Layout()
    private val lastTwoObjects = arrayOfNulls<Any>(2)
    private val spheres = mutableListOf<ModelInstance>()
    private var sphereCounter = 0
    private var draggingSphere: ModelInstance? = null

    override fun create() {
        initCamera()
        initModel()
        initInputProcessor()
        initImGui()
    }

    private fun initCamera() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera?.position?.set(20f, 20f, 20f)
        camera?.lookAt(0f, 0f, 0f)
        camera?.near = 1f
        camera?.far = 300f
        camera?.update()
    }

    private fun initImGui() {
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()
    }

    private fun initModel() {
        model = createMainPlane()
        modelInstance = ModelInstance(model)
        modelBatch = ModelBatch()
    }

    private fun initInputProcessor() {
        val inputMultiplexer = InputMultiplexer()
        val camController = MyCameraController(camera!!)

        inputMultiplexer.addProcessor(createSphereEditorProcessor(camController))
        inputMultiplexer.addProcessor(camController)
        Gdx.input.inputProcessor = inputMultiplexer
    }

    private fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
        return object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (button == Input.Buttons.LEFT) {
                    if (!status) {
                        val intersection = getIntersection(screenX, screenY)
                        if (intersection != null) {
                            draggingSphere = findSphereAt(intersection)
                            if (draggingSphere != null) {
                                camController.camaraEnabled = false
                            }
                        }
                    } else {
                        handleAddRoad(screenX, screenY)
                    }
                }
                return false
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (button == Input.Buttons.LEFT) {
                    draggingSphere = null
                    camController.camaraEnabled = true
                }
                return false
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (draggingSphere != null) {
                    val intersection = getIntersection(screenX, screenY)
                    if (intersection != null) {
                        var roadIntersection: Intersection? = null
                        for (i in spheres.indices) {
                            if (spheres[i] == draggingSphere) {
                                roadIntersection = layout.intersectionsList[i]
                            }
                        }
                        if (roadIntersection != null) {
                            roadIntersection.position.x = intersection.x.toDouble()
                            roadIntersection.position.y = intersection.y.toDouble()
                            roadIntersection.position.z = intersection.z.toDouble()
                        }
                        draggingSphere!!.transform.setToTranslation(intersection)
                    }
                }
                return false
            }
        }
    }

    private fun handleAddRoad(screenX: Int, screenY: Int) {
        val intersection = getIntersection(screenX, screenY)
        if (intersection != null) {
            if (abs(intersection.x) < 30 && abs(intersection.z) < 30) {
                val roadIntersection = findRoadIntersectionAt(intersection)
                if (roadIntersection != null) {
                    lastTwoObjects[sphereCounter] = roadIntersection

                } else {
                    val sphereModel = createSphere()
                    val sphereInstance = ModelInstance(sphereModel)
                    sphereInstance.transform.setToTranslation(intersection)
                    spheres.add(sphereInstance)
                    lastTwoObjects[sphereCounter] =
                        Vec3(intersection.x.toDouble(), intersection.y.toDouble(), intersection.z.toDouble())
                }

                sphereCounter += 1
                if (sphereCounter == 2) {
                    if (lastTwoObjects[0] is Vec3) {
                        if (lastTwoObjects[1] is Vec3) {
                            layout.addRoad(lastTwoObjects[0] as Vec3, lastTwoObjects[1] as Vec3)
                        }
                        if (lastTwoObjects[1] is Intersection) {
                            layout.addRoad(lastTwoObjects[0] as Vec3, lastTwoObjects[1] as Intersection)
                        }
                    }
                    if (lastTwoObjects[0] is Intersection) {
                        if (lastTwoObjects[1] is Vec3) {
                            layout.addRoad(lastTwoObjects[0] as Intersection, lastTwoObjects[1] as Vec3)
                        }
                        if (lastTwoObjects[1] is Intersection) {
                            layout.addRoad(lastTwoObjects[0] as Intersection, lastTwoObjects[1] as Intersection)
                        }
                    }
                    println(layout)
                    sphereCounter = 0
                    status = false
                }
            }
        }
    }

    private fun findRoadIntersectionAt(intersection: Vector3): Intersection? {
        for (i in spheres.indices) {
            if (spheres[i].transform.getTranslation(Vector3()).dst(intersection) < 0.5f) {
                return layout.intersectionsList[i]
            }
        }
        return null
    }

    private fun findSphereAt(intersection: Vector3): ModelInstance? {
        for (sphere in spheres) {
            if (sphere.transform.getTranslation(Vector3()).dst(intersection) < 0.5f) {
                return sphere
            }
        }
        return null
    }

    private fun getIntersection(screenX: Int, screenY: Int): Vector3? {
        val ray = camera!!.getPickRay(screenX.toFloat(), screenY.toFloat())
        val intersection = Vector3()
        if (Intersector.intersectRayPlane(ray, plane, intersection)) {
            return intersection
        }
        return null
    }

    private fun createSphere(): Model? {
        val modelBuilder = ModelBuilder()
        val material = Material(ColorAttribute.createDiffuse(Color.RED))
        return modelBuilder.createSphere(
            0.5f, 0.5f, 0.5f, 10,
            10, material, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    }

    private fun createMainPlane(): Model {
        val modelBuilder = ModelBuilder()
        val material = Material(ColorAttribute.createDiffuse(Color(0.5f, 0.5f, 0.5f, 0.5f)))
        return modelBuilder.createRect(
            -30f, 0f, -30f,
            30f, 0f, -30f,
            30f, 0f, 30f,
            -30f, 0f, 30f,
            0f, 1f, 0f,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    }

    override fun render() {
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camera?.update()

        modelBatch.begin(camera)
        modelBatch.render(modelInstance)
        for (sphere in spheres) {
            modelBatch.render(sphere)
        }
        modelBatch.end()

        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()
        run {
            ImGui.begin("Hello world!")
            if (ImGui.button("Add road")) {
                status = true
            }
            ImGui.end()
        }
        ImGui.render()
        imGuiGl3.renderDrawData(ImGui.getDrawData())
    }

    override fun dispose() {
        model.dispose()
        modelBatch.dispose()
    }
}
