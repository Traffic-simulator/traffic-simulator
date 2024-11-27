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
import kotlin.math.abs


class Main() : ApplicationAdapter() {
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
    private var layoutModel: Model? = null

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
                val right = halfDir.cross(Vec3(0.0, 1.0, 0.0)).normalized() * laneWidth
                node.translation.set(pos.toGdxVec())
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

            val getDistanceToSegment = { point: Vec3, from: Vec3, to: Vec3 ->
                val dir = to - from
                val t = Math.clamp((point - from).dot(dir) / dir.lengthSq(), 0.0, 1.0)
                val projected = from + dir * t
                (point - projected).length()
            }

            val intersectionBoxSize = 20.0
            val samplePerSide = 40
            val cellSize = intersectionBoxSize / (samplePerSide - 1).toDouble()
            val upVec = Vec3(0.0, roadHeight, 0.0)
            for (intersection in layout.getIntersections()) {
                val node = modelBuilder.node()
                node.translation.set(intersection.position.toGdxVec())
                meshPartBuilder = modelBuilder.part("intersection${intersection.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
                val intersectionSdf = { local: Vec3 ->
                    val point = intersection.position + local
                    var minDist = intersectionBoxSize * intersectionBoxSize
                    for (road in intersection.getIncomingRoads()) {
                        val dist = getDistanceToSegment(point, road.startIntersection.position, road.endIntersection.position)
                        if (abs(dist) < abs(minDist))
                            minDist = dist
                    }
                    minDist - laneWidth
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
                val leftBottomCorner = Vec3(-intersectionBoxSize / 2.0, 0.0, -intersectionBoxSize / 2.0)
//                val patterns = arrayOf(
//                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and !bType and !cType and !dType}
//                        to { a: Vec3, b: Vec3, c: Vec3, _: Vec3 ->
//                            val normal = (b - c).cross(Vec3(0.0, 1.0, 0.0)).normalized()
//                            insertRect((a + c) / 2.0, (a + b) / 2.0, normal)
//                        },
//                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and bType and !cType and !dType}
//                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
//                            insertRect((a + c) / 2.0, (b + d) / 2.0, (c - a).normalized())
//                        },
//                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and dType and !bType and !cType}
//                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
//                            var normal = (d - a).cross(Vec3(0.0, 1.0, 0.0)).normalized()
//                            insertRect((b + a) / 2.0, (b + d) / 2.0, normal)
//                            normal = (a - d).cross(Vec3(0.0, 1.0, 0.0)).normalized()
//                            insertRect((c + d) / 2.0, (c + a) / 2.0, normal)
//                        },
//                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and bType and cType and dType}
//                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
//                            val normal = -(b - c).cross(Vec3(0.0, 1.0, 0.0)).normalized()
//                            insertRect((a + b) / 2.0, (a + c) / 2.0, normal)
//                        },
//                )
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
                        if ((aType == bType) && (bType == cType) && (cType == dType))
                            continue
//
//                        for ((match, action) in patterns) {
//                            if (match(aType, bType, cType, dType)) {
//                                action(a, b, c, d)
//                                break;
//                            } else if (match(bType, cType, dType, aType)) {
//                                action(b, c, d, a)
//                                break;
//                            } else if (match(cType, dType, aType, bType)) {
//                                action(c, d, a, b)
//                                break;
//                            } else if (match(dType, aType, bType, cType)) {
//                                action(d, a, b, c)
//                                break;
//                            }
//                        }
                        if (aType and !bType and !cType and !dType) {
                            val normal = (b - c).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((a + c) / 2.0, (a + b) / 2.0, normal)
                            continue
                        } else if (bType and !aType and !cType and !dType) {
                            val normal = (d - a).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((b + a) / 2.0, (b + d) / 2.0, normal)
                            continue
                        } else if (cType and !aType and !bType and !dType) {
                            val normal = (a - d).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((c + d) / 2.0, (c + a) / 2.0, normal)
                            continue
                        } else if (dType and !aType and !cType and !bType) {
                            val normal = (c - b).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((d + b) / 2.0, (d + c) / 2.0, normal)
                            continue
                        }

                        if (aType and bType and !cType and !dType) {
                            insertRect((a + c) / 2.0, (b + d) / 2.0, Vec3(0.0, 0.0, 1.0))
                            continue
                        } else if (bType and dType and !aType and !cType) {
                            insertRect((a + b) / 2.0, (d + c) / 2.0, Vec3(-1.0, 0.0, 0.0))
                            continue
                        } else if (dType and cType and !aType and !bType) {
                            insertRect((b + d) / 2.0, (a + c) / 2.0, Vec3(0.0, 0.0, -1.0))
                            continue
                        } else if (aType and cType and !bType and !dType) {
                            insertRect((d + c) / 2.0, (a + b) / 2.0, Vec3(1.0, 0.0, 0.0))
                            continue
                        } else if (aType and dType and !bType and !cType) {
                            var normal = (d - a).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((b + a) / 2.0, (b + d) / 2.0, normal)
                            normal = (a - d).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((c + d) / 2.0, (c + a) / 2.0, normal)
                            continue
                        } else if (bType and cType and !aType and !dType) {
                            var normal = (b - c).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((a + c) / 2.0, (a + b) / 2.0, normal)
                            normal = (c - b).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((d + b) / 2.0, (d + c) / 2.0, normal)
                            continue
                        }

                        if (!aType and bType and cType and dType) {
                            val normal = -(b - c).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((a + b) / 2.0, (a + c) / 2.0, normal)
                            continue
                        } else if (!bType and aType and cType and dType) {
                            val normal = -(d - a).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((b + d) / 2.0, (b + a) / 2.0, normal)
                            continue
                        } else if (!cType and aType and bType and dType) {
                            val normal = -(a - d).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((c + a) / 2.0, (c + d) / 2.0, normal)
                            continue
                        } else if (!dType and aType and cType and bType) {
                            val normal = -(c - b).cross(Vec3(0.0, 1.0, 0.0)).normalized()
                            insertRect((d + c) / 2.0, (d + b) / 2.0, normal)
                            continue
                        }
                    }
                }
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

        val inter1 = layout.addIntersection(Vec3(0.0, 0.0, 0.0))
        layout.addRoad(inter1, Vec3(100.0, 0.0, 0.0))
//        layout.addRoad(Vec3(100.0, 0.0, -100.0), Vec3(100.0, 0.0, 0.0))
        layout.addRoad(inter1, Vec3(0.0, 0.0, 100.0))
//        layout.addRoad(Vec3(100.0, 0.0, 100.0), Vec3(0.0, 0.0, 100.0))
        layout.addRoad(inter1, Vec3(100.0, 0.0, 100.0))
        layout.addRoad(inter1, Vec3(-100.0, 0.0, -100.0))
//        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(-100.0, 0.0, 100.0))
//        layout.addRoad(Vec3(0.0, 0.0, 0.0), Vec3(100.0, 0.0, -100.0))
        layoutModel = createLayoutMesh(layout)
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
