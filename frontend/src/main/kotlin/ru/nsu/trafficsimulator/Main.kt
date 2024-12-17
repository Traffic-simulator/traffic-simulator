package ru.nsu.trafficsimulator

import BackendAPI
import OpenDriveReader
import SpawnDetails
import com.badlogic.gdx.*
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
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
import kotlin.math.abs
import kotlin.math.floor


class Main : ApplicationAdapter() {
    private var image: Texture? = null
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var camera: PerspectiveCamera? = null
    private var environment: Environment? = null
    private var sceneManager: SceneManager? = null
    private var sceneAsset1: SceneAsset? = null
    private var tmpInputProcessor: InputProcessor? = null
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
            val meshPartBuilder = modelBuilder.part(
                "road${road.id}",
                GL20.GL_TRIANGLES,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                Material()
            )
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
                meshPartBuilder = modelBuilder.part(
                    "intersection${road.id}",
                    GL20.GL_TRIANGLES,
                    (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                    Material()
                )

                val stepCount = floor((road.geometry!!.length - 2 * intersectionPadding) / splineRoadSegmentLen).toInt()
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
                meshPartBuilder = modelBuilder.part(
                    "intersection${intersection.id}",
                    GL20.GL_TRIANGLES,
                    (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
                    Material()
                )
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
                    { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and !bType and !cType and !dType }
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        val abPoint = getRefinedGuess(a, b)
                        val acPoint = getRefinedGuess(a, c)
                        val normal = (abPoint - acPoint).cross(Vec3(0.0, -1.0, 0.0)).normalized()
                        insertRect(acPoint, abPoint, normal)
                        insertTriangle(d + upVec, b + upVec, abPoint + upVec, upDir)
                        insertTriangle(d + upVec, abPoint + upVec, acPoint + upVec, upDir)
                        insertTriangle(d + upVec, acPoint + upVec, c + upVec, upDir)
                    },
                    { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and bType and !cType and !dType }
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
                    { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and dType and !bType and !cType }
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
                    { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and bType and cType and dType }
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        val abPoint = getRefinedGuess(a, b)
                        val acPoint = getRefinedGuess(a, c)
                        val normal = (abPoint - acPoint).cross(upDir).normalized()
                        insertRect(abPoint, acPoint, normal)
                        insertTriangle(acPoint + upVec, abPoint + upVec, a + upVec, upDir)
                    },
                    { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and !bType and !cType and !dType }
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
    private val carRotationFlags = mutableMapOf<Int, Boolean>()
    val previousGeometries = mutableMapOf<Long, TRoadPlanViewGeometry?>()

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
        sceneAsset1 = GLBLoader().load(Gdx.files.internal("racer.glb"))
        carModel = sceneAsset1?.scene?.model
        sceneManager?.setCamera(camera)
        sceneManager?.environment = environment

        val inputMultiplexer = InputMultiplexer()
        val camController = MyCameraController(camera!!)

        inputMultiplexer.addProcessor(createSphereEditorProcessor(camController))
        inputMultiplexer.addProcessor(camController)
        Gdx.input.inputProcessor = inputMultiplexer

        modelInstance1 = ModelInstance(carModel)
        modelBatch = ModelBatch()

        val odr = OpenDriveReader()
        val openDRIVE = odr.read("Town01.xodr")
        val spawnDetails = ArrayList<Triple<String, String, Direction>>()
//        spawnDetails.add(Triple("20", "1", Direction.FORWARD))
//        spawnDetails.add(Triple("11", "1", Direction.FORWARD))
        spawnDetails.add(Triple("6", "-1", Direction.FORWARD))
//        spawnDetails.add(Triple("13", "1", Direction.BACKWARD))

        back.init(openDRIVE, SpawnDetails(spawnDetails), 500)

//        layout = Deserializer().deserialize(OpenDriveReader().read("Town01.xodr"))
//        println(layout.roads)
//        for (i in layout.roads) {
//            println("${i.value.id} ${i.value.geometry}")
//        }
//        println(layout.intersectionRoads)

        /*val time = measureTime {
            layoutModel = createLayoutModel(layout_2)
        }
        println("Road layout model generation took $time")
        layoutScene = Scene(layoutModel)
        sceneManager?.addScene(layoutScene)*/

        // Add ground
        val modelBuilder = ModelBuilder()
        val groundMaterial = Material(PBRColorAttribute.createBaseColorFactor(Color(0.0f, 0.8f, 0.0f, 1.0f)))
        modelBuilder.begin()
        val meshPartBuilder = modelBuilder.part(
            "Ground",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            groundMaterial
        )
        BoxShapeBuilder.build(meshPartBuilder, 1000000.0f, 0.1f, 100000.0f)
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
    }

    private var layout: Layout = Layout()
    private var layoutScene: Scene? = null

    private var addRoadStatus = false;
    private var editRoadStatus = false;
    private var currentEditRoadId: Long = 0;
    private var deleteRoadStatus = false;
    private var editStatus = true;

    private val lastTwoAddObjects = arrayOfNulls<Any>(2)
    private var sphereAddCounter = 0
    private val lastTwoDeleteIntersections = arrayOfNulls<Intersection>(2)
    private var intersectionDeleteCounter = 0

    private val spheres = mutableListOf<ModelInstance>()
    private val directionSpheres = mutableListOf<Pair<ModelInstance, ModelInstance>>()
    private val directionSpheresMap = mutableMapOf<Long?, Pair<ModelInstance, ModelInstance>>()
    private val offsetDirectionSphere: Double = 25.0

    private var draggingSphere: ModelInstance? = null

    private fun createSphereEditorProcessor(camController: MyCameraController): InputProcessor {
        return object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (button == Input.Buttons.LEFT && editStatus) {
                    if (!addRoadStatus && !deleteRoadStatus && !editRoadStatus) {
                        val intersection = getIntersection(screenX, screenY)
                        if (intersection != null) {
                            draggingSphere = findSphereAt(intersection)
                            if (draggingSphere != null) {
                                camController.camaraEnabled = false
                            }
                        }
                    }
                    if (addRoadStatus) {
                        handleAddRoad(screenX, screenY)
                    }
                    if (deleteRoadStatus) {
                        handleDeleteRoad(screenX, screenY)
                    }
                }
                return false
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (button == Input.Buttons.LEFT && editStatus) {
                    draggingSphere = null
                    camController.camaraEnabled = true
                    updateLayout()
                }
                return false
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (draggingSphere != null && editStatus) {
                    val intersection = getIntersection(screenX, screenY)
                    if (intersection != null) {
                        var roadIntersection: Intersection? = null
                        for (i in spheres.indices) {
                            if (spheres[i] == draggingSphere) {
                                roadIntersection = layout.intersectionsList[i]
                            }
                        }
                        if (roadIntersection != null) {
                            layout.moveIntersection(
                                roadIntersection,
                                Vec3(intersection.x.toDouble(), intersection.y.toDouble(), intersection.z.toDouble())
                            )
                        }
                        draggingSphere!!.transform.setToTranslation(intersection)
                    }
                }
                return false
            }
        }
    }

    private fun handleDeleteRoad(screenX: Int, screenY: Int) {
        val intersection = getIntersection(screenX, screenY)
        if (intersection != null) {
            val roadIntersection = findRoadIntersectionAt(intersection)
            if (roadIntersection != null) {
                lastTwoDeleteIntersections[intersectionDeleteCounter] = roadIntersection
                intersectionDeleteCounter += 1
                if (intersectionDeleteCounter == 2) {
                    val road = findRoad(lastTwoDeleteIntersections[0]!!, lastTwoDeleteIntersections[1]!!)
                    if (road != null) {
                        layout.deleteRoad(road)
                    }
                    val deleteStatus1 = lastTwoDeleteIntersections[0]!!.incomingRoads.size == 0
                    val deleteStatus2 = lastTwoDeleteIntersections[1]!!.incomingRoads.size == 0
                    if (deleteStatus1) {
                        spheres.remove(findSphereAt(lastTwoDeleteIntersections[0]!!.position.toGdxVec()))
                    }
                    if (deleteStatus2) {
                        spheres.remove(findSphereAt(lastTwoDeleteIntersections[1]!!.position.toGdxVec()))
                    }
                    updateLayout()
                    intersectionDeleteCounter = 0
                    deleteRoadStatus = false
                }
            }
        }
    }

    private fun handleAddRoad(screenX: Int, screenY: Int) {
        val intersection = getIntersection(screenX, screenY)
        if (intersection != null) {
            val roadIntersection = findRoadIntersectionAt(intersection)
            if (roadIntersection != null) {
                lastTwoAddObjects[sphereAddCounter] = roadIntersection

            } else {
                val sphereModel = createSphere()
                val sphereInstance = ModelInstance(sphereModel)
                sphereInstance.transform.setToTranslation(intersection)
                spheres.add(sphereInstance)
                lastTwoAddObjects[sphereAddCounter] =
                    Vec3(intersection.x.toDouble(), intersection.y.toDouble(), intersection.z.toDouble())
            }

            sphereAddCounter += 1
            if (sphereAddCounter == 2) {
                val spherePairModel = createDirectionPairSphere()
                val startInstance = ModelInstance(spherePairModel.first)
                val endInstance = ModelInstance(spherePairModel.second)
                var startDirection: Vec3
                var endDirection: Vec3
                var roadId: Long? = null
                if (lastTwoAddObjects[0] is Vec3) {
                    startDirection = Vec3(
                        (lastTwoAddObjects[0] as Vec3).x + offsetDirectionSphere,
                        (lastTwoAddObjects[0] as Vec3).y,
                        (lastTwoAddObjects[0] as Vec3).z + offsetDirectionSphere
                    )
                    if (lastTwoAddObjects[1] is Vec3) {
                        endDirection = Vec3(
                            (lastTwoAddObjects[1] as Vec3).x + offsetDirectionSphere,
                            (lastTwoAddObjects[1] as Vec3).y,
                            (lastTwoAddObjects[1] as Vec3).z + offsetDirectionSphere
                        )
                        startInstance.transform.setToTranslation(startDirection.toGdxVec())
                        endInstance.transform.setToTranslation(endDirection.toGdxVec())
                        layout.addRoad(
                            lastTwoAddObjects[0] as Vec3,
                            startDirection,
                            lastTwoAddObjects[1] as Vec3,
                            endDirection
                        )
                        roadId = findRoadId(
                            findRoadIntersectionAt((lastTwoAddObjects[0] as Vec3).toGdxVec()),
                            findRoadIntersectionAt((lastTwoAddObjects[1] as Vec3).toGdxVec())
                        )
                    }
                    if (lastTwoAddObjects[1] is Intersection) {
                        endDirection = Vec3(
                            (lastTwoAddObjects[1] as Intersection).position.x + offsetDirectionSphere,
                            (lastTwoAddObjects[1] as Intersection).position.y,
                            (lastTwoAddObjects[1] as Intersection).position.z + offsetDirectionSphere
                        )
                        startInstance.transform.setToTranslation(startDirection.toGdxVec())
                        endInstance.transform.setToTranslation(endDirection.toGdxVec())
                        layout.addRoad(
                            lastTwoAddObjects[0] as Vec3,
                            startDirection,
                            lastTwoAddObjects[1] as Intersection,
                            endDirection
                        )
                        roadId = findRoadId(
                            findRoadIntersectionAt((lastTwoAddObjects[0] as Vec3).toGdxVec()),
                            lastTwoAddObjects[1] as Intersection
                        )
                    }
                }
                if (lastTwoAddObjects[0] is Intersection) {
                    startDirection = Vec3(
                        (lastTwoAddObjects[0] as Intersection).position.x + offsetDirectionSphere,
                        (lastTwoAddObjects[0] as Intersection).position.y,
                        (lastTwoAddObjects[0] as Intersection).position.z + offsetDirectionSphere
                    )
                    if (lastTwoAddObjects[1] is Vec3) {
                        endDirection = Vec3(
                            (lastTwoAddObjects[1] as Vec3).x + offsetDirectionSphere,
                            (lastTwoAddObjects[1] as Vec3).y,
                            (lastTwoAddObjects[1] as Vec3).z + offsetDirectionSphere
                        )
                        startInstance.transform.setToTranslation(startDirection.toGdxVec())
                        endInstance.transform.setToTranslation(endDirection.toGdxVec())
                        layout.addRoad(
                            lastTwoAddObjects[0] as Intersection,
                            startDirection,
                            lastTwoAddObjects[1] as Vec3,
                            endDirection
                        )
                        roadId = findRoadId(
                            lastTwoAddObjects[0] as Intersection,
                            findRoadIntersectionAt((lastTwoAddObjects[1] as Vec3).toGdxVec())
                        )
                    }
                    if (lastTwoAddObjects[1] is Intersection) {
                        if (lastTwoAddObjects[0] !== lastTwoAddObjects[1]) {
                            endDirection = Vec3(
                                (lastTwoAddObjects[1] as Intersection).position.x + offsetDirectionSphere,
                                (lastTwoAddObjects[1] as Intersection).position.y,
                                (lastTwoAddObjects[1] as Intersection).position.z + offsetDirectionSphere
                            )
                            startInstance.transform.setToTranslation(startDirection.toGdxVec())
                            endInstance.transform.setToTranslation(endDirection.toGdxVec())
                            layout.addRoad(
                                lastTwoAddObjects[0] as Intersection,
                                startDirection,
                                lastTwoAddObjects[1] as Intersection,
                                endDirection
                            )
                            roadId =
                                findRoadId(lastTwoAddObjects[0] as Intersection, lastTwoAddObjects[1] as Intersection)
                        }
                    }
                }
                directionSpheres.add(Pair(startInstance, endInstance))
                directionSpheresMap[roadId] = Pair(startInstance, endInstance)
                updateLayout()
                sphereAddCounter = 0
                addRoadStatus = false
            }
        }
    }

    private fun updateLayout() {
        if (layoutScene != null) {
            sceneManager?.removeScene(layoutScene)
        }
        layoutModel = createLayoutModel(layout)
        layoutScene = Scene(layoutModel)
        sceneManager?.addScene(layoutScene)
    }

    private fun findRoad(intersection1: Intersection, intersection2: Intersection): Road? {
        for (road in layout.roads.values) {
            if (road.startIntersection === intersection1 && road.endIntersection === intersection2) {
                return road
            }
            if (road.startIntersection === intersection2 && road.endIntersection === intersection1) {
                return road
            }
        }
        return null
    }

    private fun findRoadId(intersection1: Intersection?, intersection2: Intersection?): Long? {
        for (key in layout.roads.keys) {
            if (layout.roads[key]?.startIntersection === intersection1 && layout.roads[key]?.endIntersection === intersection2) {
                return key
            }
            if (layout.roads[key]?.startIntersection === intersection2 && layout.roads[key]?.endIntersection === intersection1) {
                return key
            }
        }
        return null
    }

    private fun findRoadIntersectionAt(intersection: Vector3): Intersection? {
        for (i in spheres.indices) {
            if (spheres[i].transform.getTranslation(Vector3()).dst(intersection) < 5.0f) {
                return layout.intersectionsList[i]
            }
        }
        return null
    }

    private fun findSphereAt(intersection: Vector3): ModelInstance? {
        for (sphere in spheres) {
            if (sphere.transform.getTranslation(Vector3()).dst(intersection) < 5.0f) {
                return sphere
            }
        }
        return null
    }

    private fun getIntersection(screenX: Int, screenY: Int): Vector3? {
        val ray = camera!!.getPickRay(screenX.toFloat(), screenY.toFloat())
        val intersection = Vector3()
        val plane = Plane(Vector3(0f, 1f, 0f), 0f)
        if (Intersector.intersectRayPlane(ray, plane, intersection)) {
            return intersection
        }
        return null
    }

    private fun createDirectionPairSphere(): Pair<Model, Model> {
        val modelBuilder = ModelBuilder()

        val startMaterial = Material(ColorAttribute.createDiffuse(Color.BLUE))
        val start = modelBuilder.createSphere(
            5.0f, 5.0f, 5.0f, 10,
            10, startMaterial, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        val endMaterial = Material(ColorAttribute.createDiffuse(Color.SKY))
        val end = modelBuilder.createSphere(
            5.0f, 5.0f, 5.0f, 10,
            10, endMaterial, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        return Pair(start, end)
    }

    private fun createSphere(): Model? {
        val modelBuilder = ModelBuilder()
        val material = Material(ColorAttribute.createDiffuse(Color.RED))
        val sphere = modelBuilder.createSphere(
            5.0f, 5.0f, 5.0f, 10,
            10, material, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        return sphere
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
            ImGui.begin("Editor")
            if (ImGui.button("Add road")) {
                addRoadStatus = true
                deleteRoadStatus = false
            }
            if (ImGui.button("Edit road")) {
                editRoadStatus = !editRoadStatus
            }
            if (ImGui.button("Delete road")) {
                deleteRoadStatus = true
                addRoadStatus = false
            }
            if (ImGui.button("Edit mode")) {
                editStatus = !editStatus
                println(layout)
            }
            ImGui.end()
        }
        ImGui.render()
        if (ImGui.getIO().wantCaptureKeyboard or ImGui.getIO().wantCaptureMouse) {
            tmpInputProcessor = Gdx.input.inputProcessor
            Gdx.input.inputProcessor = null
        }

        // Получаем данные о положении машинок
        val vehicleData = back.getNextFrame(0.01)
//      println("Vehicle data: $vehicleData")

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
        if (editStatus) {
            for (sphere in spheres) {
                modelBatch?.render(sphere)
            }
            if (editRoadStatus) {
                modelBatch?.render(directionSpheresMap[currentEditRoadId]!!.first)
                modelBatch?.render(directionSpheresMap[currentEditRoadId]!!.second)
            }
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
        val laneWidth = 3.5 // Ширина одной полосы в метрах

        for (vehicle in vehicleData) {
            val vehicleId = vehicle.id
            val carPosition = vehicle.distance
            val carRoad = vehicle.road
            val carDirection = vehicle.direction
            val carLaneIndex = -vehicle.laneId // Индекс полосы движения (-1, 0, 1 и т.д.)

            // Если машина не добавлена, создаем новую ModelInstance
            if (!carInstances.containsKey(vehicleId)) {
                carInstances[vehicleId] = ModelInstance(carModel)
                carRotationFlags[vehicleId] = false
                previousGeometries[vehicleId.toLong()] = null
            }

            val carInstance = carInstances[vehicleId]!!

            if (carRoad != null) {
                // Определяем текущую геометрию
                val currentGeometry: TRoadPlanViewGeometry? = carRoad.planView.geometry.firstOrNull {
                    val distanceOnRoad = if (carLaneIndex > 0) {
                        carPosition
                    } else {
                        carRoad.length - carPosition
                    }
                    distanceOnRoad in it.s..(it.s + it.length)
                }

                // Обработка перехода между геометриями для BACKWARD
                /*if (carDirection == Direction.BACKWARD && currentGeometry != null && carPosition < currentGeometry.s) {
                    val prevGeometry = carRoad.planView.geometry.lastOrNull { it.s + it.length <= currentGeometry!!.s }
                    if (prevGeometry != null) {
                        currentGeometry = prevGeometry
                        carPosition = currentGeometry.s + currentGeometry.length - (currentGeometry.s - carPosition)
                    } else {
                        carPosition = currentGeometry.s
                    }
                }*/

                /*if (vehicleId == 1) {
                    if (currentGeometry != null) {
                        println("id ${vehicleId} road ${carRoad.id} lane ${vehicle.laneId} curgeo ${currentGeometry.length} pos ${carPosition} dir ${carDirection}")
                    }
                }*/

                // Проверяем изменение геометрии
                val previousGeometry = previousGeometries[vehicleId.toLong()]
                if (currentGeometry != previousGeometry) {
                    // Геометрия изменилась, сбрасываем флажок вращения
                    carRotationFlags[vehicleId] = false
                    previousGeometries[vehicleId.toLong()] = currentGeometry
                }

                if (currentGeometry == null) continue

                // Вычисляем смещение автомобиля для текущей полосы
                val lanePositionOffset = carLaneIndex * (laneWidth / 2)

                val geometryStart = currentGeometry.s
                val geometryEnd = geometryStart + currentGeometry.length

                if (currentGeometry.line != null) {
                    // Линейная геометрия
                    val offset = if (carLaneIndex > 0) {
                        carPosition - geometryStart
                    } else {
                        geometryEnd - carPosition
                    }

                    val newPosX =
                        currentGeometry.x + offset * cos(currentGeometry.hdg) - lanePositionOffset * sin(currentGeometry.hdg)
                    val newPosZ =
                        currentGeometry.y + offset * sin(currentGeometry.hdg) + lanePositionOffset * cos(currentGeometry.hdg)
                    carInstance.transform.setTranslation(newPosX.toFloat(), 1f, newPosZ.toFloat())

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
                    val offset = if (carLaneIndex > 0) {
                        carPosition - geometryStart
                    } else {
                        geometryEnd - carPosition
                    }
                    val endAngle = startAngle + currentGeometry.arc.curvature * offset
                    val end = Point2(currentGeometry.x, currentGeometry.y) - Point2(
                        sin(startAngle) - sin(endAngle),
                        -cos(startAngle) + cos(endAngle)
                    ) * r

                    val newPosX = end.x
                    val newPosZ = end.y
                    carInstance.transform.setTranslation(newPosX.toFloat(), 1f, newPosZ.toFloat())

                    val rotationAngle = if (carLaneIndex > 0) {
                        -endAngle + Math.PI // Инвертируем направление вращения
                    } else {
                        -endAngle
                    }
                    carInstance.transform.rotateRad(Vector3(0f, 1f, 0f), rotationAngle.toFloat())
                }
            }
        }

        // Удаление машин, которых больше нет в vehicleData
        val vehicleIds = vehicleData.map { it.id }.toSet()
        val removedKeys = carInstances.keys - vehicleIds
        for (key in removedKeys) {
            carInstances.remove(key)
            carRotationFlags.remove(key) // Удаление флага при удалении машины
            previousGeometries.remove(key.toLong()) // Удаление информации о геометрии
        }
    }

}

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
