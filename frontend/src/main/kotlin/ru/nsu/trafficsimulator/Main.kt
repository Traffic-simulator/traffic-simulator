package ru.nsu.trafficsimulator;

import BackendAPI
import OpenDriveReader
import SpawnDetails
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
import opendrive.TRoad
import vehicle.Direction
import kotlin.math.*


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

    var hasRotated = false
    var currentRoad: TRoad? = null
    var currentGeometryIndex = 0

    private val back = BackendAPI()

    override fun create() {
        image = Texture("libgdx.png")
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

        // Устанавливаем камеру сверху
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera?.position?.set(-40f, 20f, 240f)  // Камера будет находиться 20 единиц выше по оси Y
        camera?.lookAt(-40f, 0.0f, 240.0f)  // Камера будет смотреть вниз, в центр сцены
        camera?.up?.set(0f, 0f, -1f)  // Устанавливаем, что "вверх" будет по оси Z, чтобы камера смотрела вниз
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

        val odr = OpenDriveReader()
        val openDRIVE = odr.read("single_segment_road.xodr")
//        println(openDRIVE.road.size)

//        val openDRIVE = odr.read("single_road.xodr")

        println(openDRIVE.road.size)

//        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
//        spawnDetails.add(Triple("0", "1", Direction.FORWARD))
//        spawnDetails.add(Triple("0", "2", Direction.FORWARD))
//        spawnDetails.add(Triple("0", "3", Direction.FORWARD))
//        spawnDetails.add(Triple("0", "4", Direction.FORWARD))

        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
        spawnDetails.add(Triple("57", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("57", "2", Direction.BACKWARD))
        spawnDetails.add(Triple("21", "-1", Direction.FORWARD))
        spawnDetails.add(Triple("21", "-2", Direction.FORWARD))

        /*val spawnDetails = ArrayList<Triple<String, String, Direction>>()
        spawnDetails.add(Triple("0", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("0", "2", Direction.BACKWARD))
        spawnDetails.add(Triple("0", "3", Direction.BACKWARD))
        spawnDetails.add(Triple("0", "4", Direction.BACKWARD))
        spawnDetails.add(Triple("0", "-1", Direction.FORWARD))
        spawnDetails.add(Triple("0", "-2", Direction.FORWARD))*/

//        Еще один пример для карты без перекрестка
        back.init(openDRIVE, SpawnDetails(spawnDetails), 228)

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
        modelInstance2?.transform?.setTranslation(5 * sin(elapsedTime + 1), carY[0], 5 * cos(elapsedTime + 1))
        modelInstance2?.transform?.rotate(Vector3(0f, 1f, 0f), rotationAngle)

        // Получение данных о положении автомобиля
        val vehiclePositions = back.getNextFrame(0.005) // Интервал времени
        val carPosition = vehiclePositions[0].distance
        val newRoad = vehiclePositions[0].road

        // Если дорога изменилась, обновляем CarController
        if (currentRoad != newRoad) {
            currentRoad = newRoad
        }

        // Обновляем позицию автомобиля
        if (currentRoad != null) {
            val roadLength = currentRoad!!.length
            if (carPosition <= roadLength) {
                val currentGeometry = currentRoad!!.planView.geometry[currentGeometryIndex]

                val geometryStart = currentGeometry.s
                val geometryEnd = geometryStart + currentGeometry.length
                if (carPosition in geometryStart..geometryEnd) {
                    if (currentGeometry.line != null) {
                        val roadX = currentGeometry.x
                        val roadZ = currentGeometry.y
                        val roadAngle = currentGeometry.hdg

                        val newPosX = roadX + (carPosition - currentGeometry.s) * cos(roadAngle)
                        val newPosZ = roadZ + (carPosition - currentGeometry.s) * sin(roadAngle)

                        modelInstance1?.transform?.setTranslation(newPosX.toFloat(), carY[0], newPosZ.toFloat())

                        // Установка вращения автомобиля (если необходимо)
                        if (!hasRotated) {
                            val roadAngle = currentRoad!!.planView.geometry[currentGeometryIndex].hdg
                            modelInstance1?.transform?.setToRotationRad(Vector3(0f, 1f, 0f), -roadAngle.toFloat())
                            hasRotated = true
                        }
                    } else if (currentGeometry.arc != null) {
                        val r = 1 / currentGeometry.arc.curvature
                        val startAngle = currentGeometry.hdg
                        val offset = carPosition - currentGeometry.s
                        val endAngle = startAngle + currentGeometry.arc.curvature * offset

                        val end = Point2(currentGeometry.x, currentGeometry.y) - Point2(
                            sin(startAngle) - sin(endAngle),
                            -cos(startAngle) + cos(endAngle)
                        ) * r

                        val newPosX = end.x
                        val newPosZ = end.y

                        modelInstance1?.transform?.setTranslation(newPosX.toFloat(), carY[0], newPosZ.toFloat())

                        // Установка вращения автомобиля (если необходимо)
                        if (!hasRotated) {
                            val roadAngle = currentRoad!!.planView.geometry[currentGeometryIndex].hdg
                            modelInstance1?.transform?.setToRotationRad(Vector3(0f, 1f, 0f), -roadAngle.toFloat())
                            hasRotated = true
                        }
                    } else {
                        null
                    }
                } else {
                    // Если достигнут конец текущей геометрии, переключаемся на следующую
                    if (currentGeometryIndex < currentRoad!!.planView.geometry.size - 1) {
                        currentGeometryIndex++
                        hasRotated = false // Сбрасываем флаг вращения
                        println("\nконец геометрии ${currentGeometry.s}\n")
                    } else {
                        println("Машина достигла конца дороги ${currentRoad!!.id}")
                    }
                }
            } else {
                println("Машина достигла конца дороги ${currentRoad!!.id}")
            }
        }

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

data class Point2(var x: Double, var y: Double) {
    fun distance(other: Point2): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun rotate(angleRad: Double): Point2 {
        return Point2(x * cos(angleRad) - y * sin(angleRad), x * sin(angleRad) + y * cos(angleRad))
    }

    operator fun plus(other: Point2): Point2 {
        return Point2(x + other.x, y + other.y)
    }

    operator fun minus(other: Point2): Point2 {
        return Point2(x - other.x, y - other.y)
    }

    operator fun times(scalar: Double): Point2 {
        return Point2(x * scalar, y * scalar)
    }
}
