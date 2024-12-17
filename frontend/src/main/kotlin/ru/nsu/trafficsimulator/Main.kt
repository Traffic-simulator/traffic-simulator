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

    companion object {
        private val roadHeight = 1.0
        private val laneWidth = 3.5
        private val intersectionPadding = 0.0
        private val splineRoadSegmentLen = 2.0f
        private val upVec = Vec3(0.0, roadHeight, 0.0)

        fun buildStraightRoad(modelBuilder: ModelBuilder, road: Road) {
            val node = modelBuilder.node()
            val pos = (road.startIntersection!!.position + road.endIntersection!!.position) / 2.0
            val dir = road.endIntersection!!.position - road.startIntersection!!.position
            val halfLen = dir.length() / 2.0 - intersectionPadding
            if (halfLen < 0)
                return
            val halfDir = dir.normalized() * halfLen
            val right = halfDir.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
            val left = -halfDir.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
            node.translation.set(pos.toGdxVec())
            val meshPartBuilder = modelBuilder.part("road${road.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
            BoxShapeBuilder.build(
                meshPartBuilder,
                Vector3((-halfDir.x + left.x).toFloat(), roadHeight.toFloat(), (-halfDir.z + left.z).toFloat()),
                Vector3((-halfDir.x + right.x).toFloat(), roadHeight.toFloat(), (-halfDir.z + right.z).toFloat()),
                Vector3((halfDir.x + left.x).toFloat(), roadHeight.toFloat(), (halfDir.z + left.z).toFloat()),
                Vector3((halfDir.x + right.x).toFloat(), roadHeight.toFloat(), (halfDir.z + right.z).toFloat()),
                Vector3((-halfDir.x + left.x).toFloat(), 0.0f, (-halfDir.z + left.z).toFloat()),
                Vector3((-halfDir.x + right.x).toFloat(), 0.0f, (-halfDir.z + right.z).toFloat()),
                Vector3((halfDir.x + left.x).toFloat(), 0.0f, (halfDir.z + left.z).toFloat()),
                Vector3((halfDir.x + right.x).toFloat(), 0.0f, (halfDir.z + right.z).toFloat()),
            )
        }

        fun createLayoutModel(layout: Layout): Model {
            val modelBuilder = ModelBuilder()
            var meshPartBuilder: MeshPartBuilder
            modelBuilder.begin()
            for (road in layout.roads.values) {
                if (road.geometry == null) {
                    buildStraightRoad(modelBuilder, road)
                    continue
                }
                val node = modelBuilder.node()
                node.translation.set(0.0f, 0.0f, 0.0f)
                meshPartBuilder = modelBuilder.part("intersection${road.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())

                val stepCount = floor((road.geometry!!.length - 2 * intersectionPadding )/ splineRoadSegmentLen).toInt()
                var prevPos = road.geometry!!.getPoint(intersectionPadding).toVec3()
                var prevDir = road.geometry!!.getDirection(intersectionPadding).toVec3().normalized()
                var prevRight = prevDir.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
                var prevLeft = -prevDir.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
                meshPartBuilder.rect(
                    (prevPos + prevLeft).toGdxVec(),
                    (prevPos + prevRight).toGdxVec(),
                    (prevPos + prevRight + upVec).toGdxVec(),
                    (prevPos + prevLeft + upVec).toGdxVec(),
                    -prevDir.toGdxVec()
                )

                for (i in 1..stepCount) {
                    val t = intersectionPadding + i * splineRoadSegmentLen.toDouble()
                    val pos = road.geometry!!.getPoint(t).toVec3()
                    val direction = road.geometry!!.getDirection(t).toVec3().normalized()
                    val right = direction.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
                    val left = -direction.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
                    if (direction.dot(prevDir) < 0.0) {
                        prevLeft = prevRight.also { prevRight = prevLeft }
                    }
                    // Top
                    meshPartBuilder.rect(
                        (prevPos + prevRight + upVec).toGdxVec(),
                        (pos + right + upVec).toGdxVec(),
                        (pos + left + upVec).toGdxVec(),
                        (prevPos + prevLeft + upVec).toGdxVec(),
                        Vec3.UP.toGdxVec()
                    )
                    // Right
                    meshPartBuilder.rect(
                        (prevPos + prevRight).toGdxVec(),
                        (pos + right).toGdxVec(),
                        (pos + right + upVec).toGdxVec(),
                        (prevPos + prevRight + upVec).toGdxVec(),
                        (pos - prevPos).cross(Vec3.UP).toGdxVec()
                    )

                    // Right
                    meshPartBuilder.rect(
                        (pos + left).toGdxVec(),
                        (prevPos + prevLeft).toGdxVec(),
                        (prevPos + prevLeft + upVec).toGdxVec(),
                        (pos + left + upVec).toGdxVec(),
                        -(pos - prevPos).cross(Vec3.UP).toGdxVec()
                    )
                    prevPos = pos
                    prevLeft = left
                    prevRight = right
                    prevDir = direction
                }
                val t = road.geometry!!.length - intersectionPadding
                val pos = road.geometry!!.getPoint(t).toVec3()
                val direction = road.geometry!!.getDirection(t).toVec3().normalized()
                val right = direction.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
                val left = -direction.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
                meshPartBuilder.rect(
                    (prevPos + prevRight + upVec).toGdxVec(),
                    (pos + right + upVec).toGdxVec(),
                    (pos + left + upVec).toGdxVec(),
                    (prevPos + prevLeft + upVec).toGdxVec(),
                    Vec3.UP.toGdxVec()
                )
                // Right
                meshPartBuilder.rect(
                    (prevPos + prevRight).toGdxVec(),
                    (pos + right).toGdxVec(),
                    (pos + right + upVec).toGdxVec(),
                    (prevPos + prevRight + upVec).toGdxVec(),
                    (pos - prevPos).cross(Vec3.UP).toGdxVec()
                )

                // Right
                meshPartBuilder.rect(
                    (pos + left).toGdxVec(),
                    (prevPos + prevLeft).toGdxVec(),
                    (prevPos + prevLeft + upVec).toGdxVec(),
                    (pos + left + upVec).toGdxVec(),
                    -(pos - prevPos).cross(Vec3.UP).toGdxVec()
                )

                meshPartBuilder.rect(
                    (pos + right).toGdxVec(),
                    (pos + left).toGdxVec(),
                    (pos + left + upVec).toGdxVec(),
                    (pos + right + upVec).toGdxVec(),
                    direction.toGdxVec()
                )
            }

            val getDistanceToSegment = { point: Vec3, from: Vec3, to: Vec3 ->
                val dir = to - from
                val t = Math.clamp((point - from).dot(dir) / dir.lengthSq(), 0.0, 1.0)
                val projected = from + dir * t
                (point - projected).length()
            }

            val intersectionBoxSize = 25.0
            val samplePerSide = 40
            val cellSize = intersectionBoxSize / (samplePerSide - 1).toDouble()
            val upDir = Vec3(0.0, 1.0, 0.0)
            for (intersection in layout.intersections.values) {
                val node = modelBuilder.node()
                node.translation.set(intersection.position.toGdxVec())
                meshPartBuilder = modelBuilder.part("intersection${intersection.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
                val intersectionSdf = { local: Vec3 ->
                    val point = intersection.position + local
                    var minDist = intersectionBoxSize * intersectionBoxSize
                    var laneCount = 1
                    for (road in intersection.intersectionRoads) {
                        val dist = if (road.geometry != null) {
                            val proj = Vec2(point.x, point.z)
                            (road.geometry.closestPoint(proj) - proj).length()
                        } else {
                            minDist
                        }
                        if (abs(dist) < abs(minDist)) {
                            minDist = dist
                            laneCount = 1
                        }
                    }
                    minDist - laneWidth * laneCount
                }
                val insertRect = { a: Vec3, b: Vec3, normal: Vec3 ->
                    meshPartBuilder.rect(
                        a.toGdxVec(),
                        (a + upVec).toGdxVec(),
                        (b + upVec).toGdxVec(),
                        b.toGdxVec(),
                        normal.toGdxVec()
                    )
                }
                val insertTriangle = { a: Vec3, b: Vec3, c: Vec3, normal: Vec3 ->
                    meshPartBuilder.triangle(
                        MeshPartBuilder.VertexInfo().set(
                            a.toGdxVec(),
                            normal.toGdxVec(),
                            null,
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            b.toGdxVec(),
                            normal.toGdxVec(),
                            null,
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            c.toGdxVec(),
                            normal.toGdxVec(),
                            null,
                            null
                        ),
                    )
                }
                val getRefinedGuess = { a: Vec3, b: Vec3 ->
                    val iterationCount = 5
                    var guess = (a + b) / 2.0
                    var left = a
                    var right = b
                    var guessType = intersectionSdf(guess) > 0
                    val leftType = intersectionSdf(left) > 0
                    val rightType = intersectionSdf(right) > 0
                    assert(leftType != rightType)
                    for (i in 0..<iterationCount) {
                        if (guessType == rightType) {
                            right = guess
                            guess = (left + guess) / 2.0
                        } else {
                            left = guess
                            guess = (right + guess) / 2.0
                        }
                        guessType = intersectionSdf(guess) > 0
                    }
                    guess
                }
                val leftBottomCorner = Vec3(-intersectionBoxSize / 2.0, 0.0, -intersectionBoxSize / 2.0)
                val patterns = arrayOf(
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and !bType and !cType and !dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                            val abPoint = getRefinedGuess(a, b)
                            val acPoint = getRefinedGuess(a, c)
                            val normal = (abPoint - acPoint).cross(Vec3(0.0, -1.0, 0.0)).normalized()
                            insertRect(acPoint, abPoint, normal)
                            insertTriangle(d + upVec, b + upVec, abPoint + upVec, upDir)
                            insertTriangle(d + upVec, abPoint + upVec, acPoint + upVec, upDir)
                            insertTriangle(d + upVec, acPoint + upVec, c + upVec, upDir)
                        },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and bType and !cType and !dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                            val bdPoint = getRefinedGuess(b, d)
                            val acPoint = getRefinedGuess(a, c)
                            val normal = -(bdPoint - acPoint).cross(upDir).normalized()
                            insertRect(acPoint, bdPoint, normal)
                            meshPartBuilder.rect(
                                (acPoint + upVec).toGdxVec(),
                                (c + upVec).toGdxVec(),
                                (d + upVec).toGdxVec(),
                                (bdPoint + upVec).toGdxVec(),
                                upDir.toGdxVec()
                            )
                        },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and dType and !bType and !cType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                            val abPoint = getRefinedGuess(a, b)
                            val acPoint = getRefinedGuess(a, c)
                            val bdPoint = getRefinedGuess(b, d)
                            val cdPoint = getRefinedGuess(c, d)
                            var normal = (abPoint - cdPoint).cross(upDir).normalized()
                            insertRect(abPoint, bdPoint, normal)
                            normal = (cdPoint - abPoint).cross(upDir).normalized()
                            insertRect(cdPoint, acPoint, normal)
                        },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and bType and cType and dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                            val abPoint = getRefinedGuess(a, b)
                            val acPoint = getRefinedGuess(a, c)
                            val normal = (abPoint - acPoint).cross(upDir).normalized()
                            insertRect(abPoint, acPoint, normal)
                            insertTriangle(acPoint + upVec, abPoint + upVec, a + upVec, upDir)
                        },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and !bType and !cType and !dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                            meshPartBuilder.rect(
                                (a + upVec).toGdxVec(),
                                (c + upVec).toGdxVec(),
                                (d + upVec).toGdxVec(),
                                (b + upVec).toGdxVec(),
                                upDir.toGdxVec()
                            )
                        },
                )
                for (i in 0..<(samplePerSide - 1)) {
                    for (j in 0..<(samplePerSide - 1)) {
                        val a = leftBottomCorner + Vec3(i * cellSize, 0.0, j * cellSize)
                        val b = leftBottomCorner + Vec3((i + 1) * cellSize, 0.0, j * cellSize)
                        val c = leftBottomCorner + Vec3(i * cellSize, 0.0, (j + 1) * cellSize)
                        val d = leftBottomCorner + Vec3((i + 1) * cellSize, 0.0, (j + 1) * cellSize)
                        val aSample = intersectionSdf(a)
                        val bSample = intersectionSdf(b)
                        val cSample = intersectionSdf(c)
                        val dSample = intersectionSdf(d)
                        val aType = aSample > 0.0
                        val bType = bSample > 0.0
                        val cType = cSample > 0.0
                        val dType = dSample > 0.0

                        for ((match, action) in patterns) {
                            if (match(aType, bType, cType, dType)) {
                                action(a, b, c, d)
                                break
                            } else if (match(cType, aType, dType, bType)) {
                                action(c, a, d, b)
                                break
                            } else if (match(bType, dType, aType, cType)) {
                                action(b, d, a, c)
                                break
                            } else if (match(dType, cType, bType, aType)) {
                                action(d, c, b, a)
                                break
                            }
                        }
                    }
                }
                System.gc()
            }
            return modelBuilder.end()
        }
    }

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
        spawnDetails.add(Triple("1", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("4", "1", Direction.BACKWARD))
        spawnDetails.add(Triple("4", "-1", Direction.FORWARD))
        spawnDetails.add(Triple("15", "-1", Direction.FORWARD))
        spawnDetails.add(Triple("12", "-1", Direction.FORWARD))
//        spawnDetails.add(Triple("6", "1", Direction.FORWARD))
//        spawnDetails.add(Triple("13", "1", Direction.BACKWARD))

        back.init(openDRIVE, SpawnDetails(spawnDetails), 500)

        layout = Deserializer.deserialize(OpenDriveReader().read("Town01.xodr"))

        val time = measureTime {
            layoutModel = createLayoutModel(layout)
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
            carInstance.transform.setToRotationRad(Vector3(0.0f, 1.0f, 0.0f), angle.toFloat())
                .setTranslation((pos.x + laneOffset * right.x * Main.laneWidth).toFloat(), 1.0f, (pos.y + laneOffset * right.z * Main.laneWidth).toFloat())
        }

        // Удаление машин, которых больше нет в vehicleData
        val vehicleIds = vehicleData.map { it.id }.toSet()
        val removedKeys = carInstances.keys - vehicleIds
        for (key in removedKeys) {
            carInstances.remove(key)
        }
    }
}
