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
import opendrive.TRoadPlanViewGeometry
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
    private var tmpInputProcessor: InputProcessor? = null

    private var carModel: Model? = null
    private var modelInstance1: ModelInstance? = null
    private var modelBatch: ModelBatch? = null

    private val back = BackendAPI()
    private val carInstances = mutableMapOf<Int, ModelInstance>()
    private val carRotationFlags = mutableMapOf<Int, Boolean>()

    override fun create() {
        image = Texture("libgdx.png")
        val windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        ImGui.createContext()
        ImGui.styleColorsDark()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init()

        // Устанавливаем камеру сверху
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera?.position?.set(-20f, 20f, 240f)  // Камера будет находиться 20 единиц выше по оси Y
        camera?.lookAt(-20f, 0.0f, 240.0f)  // Камера будет смотреть вниз, в центр сцены
        camera?.up?.set(0f, 0f, -1f)  // Устанавливаем, что "вверх" будет по оси Z, чтобы камера смотрела вниз
        camera?.near = 1f
        camera?.far = 300f
        camera?.update()

        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, 0.8f, -0.2f))

        sceneManager = SceneManager()
        sceneAsset1 = GLBLoader().load(Gdx.files.internal("racer.glb"))
        carModel = sceneAsset1?.scene?.model
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment

        modelInstance1 = ModelInstance(carModel)
        modelBatch = ModelBatch()

        val odr = OpenDriveReader()
        val openDRIVE = odr.read("single_segment_road.xodr")

        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
//        spawnDetails.add(Triple("21", "1", Direction.BACKWARD))
//        spawnDetails.add(Triple("21", "2", Direction.BACKWARD))
        spawnDetails.add(Triple("21", "-1", Direction.FORWARD))
        spawnDetails.add(Triple("21", "-2", Direction.FORWARD))

        /*val openDRIVE = odr.read("UC_Simple-X-Junction.xodr")
        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
        spawnDetails.add(Triple("0", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("1", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("6", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("13", "1", Direction.BACKWARD))*/

//        val simulator: Simulator = Simulator(openDRIVE, SpawnDetails(spawnDetails), 228);

//        Вот пример spawnDetails для UC_Simple-X-Junction.xodr, который лежит у нас в ресурсах

        /*val openDRIVE = odr.read("Town01.xodr")
        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
        spawnDetails.add(Triple("20", "1", Direction.FORWARD))
        spawnDetails.add(Triple("11", "1", Direction.FORWARD))
//        spawnDetails.add(Triple("6", "1", Direction.BACKWARD))
//        spawnDetails.add(Triple("13", "1", Direction.BACKWARD))*/

        back.init(openDRIVE, SpawnDetails(spawnDetails), 500)

        val camController = CameraInputController(camera)
        Gdx.input.inputProcessor = camController
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
        val vehicleData = back.getNextFrame(0.005)
//        println("Vehicle data: $vehicleData")

        // Обновляем позиции машинок
        updateCars(vehicleData)

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        //sceneManager?.update(Gdx.graphics.deltaTime)
        sceneManager?.render()

        modelBatch?.begin(camera)
        for (car in carInstances.values) {
            modelBatch?.render(car, environment)
        }
        modelBatch?.end()

        ImGui.render()
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
        val laneWidth = 3.5 // Ширина одной полосы в метрах

        for (vehicle in vehicleData) {
            val vehicleId = vehicle.id
            var carPosition = vehicle.distance
            val carRoad = vehicle.road
            val carDirection = vehicle.direction
            val carLaneIndex = vehicle.laneId // Индекс полосы движения (-1, 0, 1 и т.д.)

            // Если машина не добавлена, создаем новую ModelInstance
            if (!carInstances.containsKey(vehicleId)) {
                carInstances[vehicleId] = ModelInstance(carModel)
                carRotationFlags[vehicleId] = false
            }

            val carInstance = carInstances[vehicleId]!!

            if (carRoad != null) {
                // Определяем текущую геометрию
                var currentGeometry: TRoadPlanViewGeometry? = carRoad.planView.geometry.firstOrNull {
                    val distanceOnRoad = if (carDirection == Direction.FORWARD) {
                        carPosition
                    } else {
                        carRoad.length - carPosition
                    }
                    distanceOnRoad in it.s..(it.s + it.length)
                }

                // Обработка перехода между геометриями для BACKWARD
                if (carDirection == Direction.BACKWARD && currentGeometry != null && carPosition < currentGeometry.s) {
                    val prevGeometry = carRoad.planView.geometry.lastOrNull { it.s + it.length <= currentGeometry!!.s }
                    if (prevGeometry != null) {
                        currentGeometry = prevGeometry
                        carPosition = currentGeometry.s + currentGeometry.length - (currentGeometry.s - carPosition)
                    } else {
                        carPosition = currentGeometry.s
                    }
                }

                if (currentGeometry == null) continue

                // Вычисляем смещение автомобиля для текущей полосы
                val lanePositionOffset = carLaneIndex * (laneWidth / 2)

                val geometryStart = currentGeometry.s
                val geometryEnd = geometryStart + currentGeometry.length

                if (currentGeometry.line != null) {
                    // Линейная геометрия
                    val offset = if (carDirection == Direction.FORWARD) {
                        carPosition - geometryStart
                    } else {
                        geometryEnd - carPosition
                    }

                    val newPosX = currentGeometry.x + offset * cos(currentGeometry.hdg) - lanePositionOffset * sin(currentGeometry.hdg)
                    val newPosZ = currentGeometry.y + offset * sin(currentGeometry.hdg) + lanePositionOffset * cos(currentGeometry.hdg)
                    carInstance.transform.setTranslation(newPosX.toFloat(), 0f, newPosZ.toFloat())

                    val rotationAngle = if (carDirection == Direction.FORWARD) {
                        currentGeometry.hdg
                    } else {
                        currentGeometry.hdg + Math.PI
                    }
                    if (carRotationFlags[vehicleId] == false) {
                        carInstance.transform.setToRotationRad(Vector3(0f, 1f, 0f), -rotationAngle.toFloat())
                        carRotationFlags[vehicleId] = true
                    }

                } else if (currentGeometry.arc != null) {
                    // Геометрия дуги
                    val r = 1 / currentGeometry.arc.curvature
                    val startAngle = currentGeometry.hdg
                    val offset = if (carDirection == Direction.FORWARD) {
                        carPosition - geometryStart
                    } else {
                        geometryEnd - carPosition
                    }
                    val endAngle = startAngle + currentGeometry.arc.curvature * offset

                    val centerX = currentGeometry.x - r * sin(startAngle)
                    val centerY = currentGeometry.y + r * cos(startAngle)

                    val newPosX = centerX + (r - lanePositionOffset) * sin(endAngle)
                    val newPosZ = centerY - (r - lanePositionOffset) * cos(endAngle)
                    carInstance.transform.setTranslation(newPosX.toFloat(), 0f, newPosZ.toFloat())

                    val rotationAngle = if (carDirection == Direction.FORWARD) {
                        -endAngle
                    } else {
                        -endAngle + Math.PI
                    }
                    carInstance.transform.rotateRad(Vector3(0f, 1f, 0f),
                        (rotationAngle.toFloat() / 20 / currentGeometry.length).toFloat()
                    )
                }
            }
        }

        // Удаление машин, которых больше нет в vehicleData
        val vehicleIds = vehicleData.map { it.id }.toSet()
        val removedKeys = carInstances.keys - vehicleIds
        for (key in removedKeys) {
            carInstances.remove(key)
            carRotationFlags.remove(key) // Удаление флага при удалении машины
        }
    }

}

data class Point2(var x: Double, var y: Double) {
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
