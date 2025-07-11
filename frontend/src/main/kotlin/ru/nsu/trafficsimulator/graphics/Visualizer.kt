package ru.nsu.trafficsimulator.graphics

import ISimulation
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.collision.BoundingBox
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.SceneSkybox
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Vehicle
import ru.nsu.trafficsimulator.model.intsettings.BuildingType
import signals.SignalState
import java.util.HashMap
import kotlin.math.*

class Visualizer(private var layout: Layout) {
    private val AMBIENT_LIGHT_INTENSITY = 0.2f
    private val sceneManager = SceneManager()
    private val camera: PerspectiveCamera

    private val carModels = listOf(
        pathToModel("models/racer_big.glb"),
        pathToModel("models/Ambulance.glb"),
        pathToModel("models/Mazda RX-7.glb"),
        pathToModel("models/Convertible.glb"),
        pathToModel("models/cartoon banana car.glb"),
        pathToModel("models/Jeep.glb"),
        pathToModel("models/Car.glb"),
    )
    private val buildingSettings: HashMap<BuildingType, Triple<Model, Float, Float>> = hashMapOf(
        BuildingType.HOME to Triple(pathToModel("models/HOME.glb"), 2.0f, 0.0f),
        BuildingType.SHOPPING to Triple(pathToModel("models/SHOPPING.glb"), 10.0f, 0.0f),
        BuildingType.EDUCATION to Triple(pathToModel("models/EDUCATION.glb"), 2.0f, 0.0f),
        BuildingType.WORK to Triple(pathToModel("models/WORK.glb"), 15.0f, 1.0f),
        BuildingType.ENTERTAINMENT to Triple(pathToModel("models/ENTERTAINMENT.glb"), 10.0f, 1.0f)
    )

    private val buildingModel = GLBLoader().load(Gdx.files.internal("models/building.glb"))!!.scene.model
    private var trafficLightModel: Model =
        GLBLoader().load(Gdx.files.internal("models/traffic_light.glb")).scene!!.model

    private val carInstances = mutableMapOf<Int, Scene>()
    private val buildingScenes = mutableListOf<Scene>()
    private val trafficLights = mutableMapOf<Pair<Road, Boolean>, Scene>()

    private val modelBatch = ModelBatch()

    private var selectedItem: Any? = null

    private var layoutScene: Scene? = null

    var heatmapMode: Boolean = false
        set(value) {
            if (field && !value) {
                turnOffHeatmap()
            }
            field = value
        }

    init {
        val environment = Environment()
        environment.add(
            DirectionalLight().set(
                0.9f,
                0.9f,
                0.9f,
                -0f,
                -1.0f,
                -0.2f
            )
        )
        environment.set(PBRColorAttribute.createAmbientLight(
            AMBIENT_LIGHT_INTENSITY,
            AMBIENT_LIGHT_INTENSITY,
            AMBIENT_LIGHT_INTENSITY,
            1.0f
        ))
        sceneManager.environment = environment

        sceneManager.skyBox = SceneSkybox(
            Cubemap(
                Gdx.files.internal("skybox/right.png"),
                Gdx.files.internal("skybox/left.png"),
                Gdx.files.internal("skybox/top.png"),
                Gdx.files.internal("skybox/bottom.png"),
                Gdx.files.internal("skybox/front.png"),
                Gdx.files.internal("skybox/back.png"),
            )
        )

        camera = PerspectiveCamera(66f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position?.set(170f, 20f, -170f)
        camera.lookAt(170f, 0.0f, -170.0f)
        camera.near = 10.0f
        camera.far = 1000f
        camera.update()
        sceneManager.camera = camera

        sceneManager.setShaderProvider(CustomShaderProvider(
            "shaders/road.vs.glsl",
            "shaders/road.fs.glsl",
            "shaders/pbr.vs.glsl",
            "shaders/pbr.fs.glsl"
        ))

        // Add ground
        val modelBuilder = ModelBuilder()
        val groundMaterial = Material(PBRColorAttribute.createBaseColorFactor(Color(0.0f, 1.0f, 0.0f, 1.0f)))
        modelBuilder.begin()
        val meshPartBuilder = modelBuilder.part(
            "Ground",
            GL20.GL_TRIANGLES,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
            groundMaterial
        )
        BoxShapeBuilder.build(meshPartBuilder, 1000.0f, 0.1f, 1000.0f)
        val ground = modelBuilder.end()
        sceneManager.addScene(Scene(ground))

        val validAttributes = setOf(
            PBRColorAttribute.BaseColorFactor,
            PBRFloatAttribute.Metallic,
            PBRFloatAttribute.Roughness
        )

        for (material in trafficLightModel.materials) {
            val toRemove = mutableSetOf<Long>()
            for (attribute in material) {
                if (!validAttributes.contains(attribute.type)) {
                    toRemove.add(attribute.type)
                }
            }
            for (attribute in toRemove) {
                material.remove(attribute)
            }
        }
    }

    private fun pathToModel(path: String): Model {
        return GLBLoader().load(Gdx.files.internal(path))!!.scene.model
    }

    fun getCamera() = camera
    fun getModelBatch() = modelBatch

    fun render(dt: Float) {
        sceneManager.update(dt)
        sceneManager.render()
    }

    fun cleanup() {
        for (car in carInstances.values) {
            sceneManager.removeScene(car)
        }
        carInstances.clear()

        turnOffHeatmap()
    }

    fun updateCars(cars: List<Vehicle>) {
        for (vehicle in cars) {
            val vehicleId = vehicle.id

            // Если машина не добавлена, создаем новую ModelInstance
            if (!carInstances.containsKey(vehicleId)) {
                val carModel = carModels.random()
                carInstances[vehicleId] = Scene(carModel)
                sceneManager.addScene(carInstances[vehicleId])
            }
            val carInstance = carInstances[vehicleId]!!

            carInstance.modelInstance.transform.set(vehicle.transform)
        }

        // Удаление машин, которых больше нет в vehicleData
        val vehicleIds = cars.map { it.id }.toSet()
        val removedKeys = carInstances.keys - vehicleIds
        for (key in removedKeys) {
            sceneManager.removeScene(carInstances[key])
            carInstances.remove(key)
        }
    }

    fun updateSignals(signals: List<ISimulation.SignalDTO>) {
        for (signalScene in trafficLights.values) {
            val signalModel = signalScene.modelInstance
            for (material in signalModel.materials) {
                material.remove(PBRColorAttribute.Emissive)
                material.remove(PBRFloatAttribute.EmissiveIntensity)
            }
        }
        for (signal in signals) {
            val roadId = signal.road.id.toLong()
            if (!layout.roads.containsKey(roadId)) {
                logger.warn { "Failed to find road with id from SignalDTO" }
                continue
            }
            val road = layout.roads[roadId]!!

            val isStart = if (abs(signal.distFromLaneStart - road.length) < 1e-3) {
                false
            } else if (abs(signal.distFromLaneStart) < 1e-3) {
                true
            } else {
                logger.warn { "distFromLaneStart was ${signal.distFromLaneStart} which is inconclusive" }
                continue
            }

            val signalModel = trafficLights[road to isStart]!!.modelInstance

            val lightNames = when (signal.state) {
                SignalState.RED -> setOf("light-red")
                SignalState.GREEN -> setOf("color-green")
                SignalState.YELLOW -> setOf("light-yellow")
                SignalState.RED_YELLOW -> setOf("light-yellow", "light-red")
            }

            for (lightName in lightNames) {
                for (material in signalModel.materials) {
                    if (!lightNames.contains(material.id)) {
                        continue
                    }
                    var color: Color? = null
                    for (attribute in material) {
                        if (attribute.type == PBRColorAttribute.BaseColorFactor) {
                            color = (attribute as PBRColorAttribute).color
                        }
                    }
                    material.set(PBRColorAttribute.createEmissive(color))
                    material.set(PBRFloatAttribute.createEmissiveIntensity(1.0f))
                }
            }
        }
    }

    fun updateHeatmap(segments: List<ISimulation.SegmentDTO>) {
        if (!heatmapMode) {
            return
        }
        if (layoutScene == null || layoutScene!!.modelInstance.model.meshes.isEmpty) {
            return
        }

        // Road id to Lane id
        val segmentMap = mutableMapOf<Pair<Long, Int>, ISimulation.SegmentDTO>()
        for (segment in segments) {
            segmentMap[segment.road.id.toLong() to segment.laneId] = segment
        }

        data class MeshData(val mesh: Mesh) {
            val vertices = FloatArray(mesh.numVertices * mesh.vertexSize / 4)
            val indices = ShortArray(mesh.numIndices)

            private val attributes = mesh.vertexAttributes
            val colorAttrib = attributes.findByUsage(VertexAttributes.Usage.ColorUnpacked)
            val heatmapAttrib = attributes.findByUsage(VertexAttributes.Usage.Generic)

            init {
                mesh.getVertices(vertices)
                mesh.getIndices(indices)
            }
        }
        val nodes = layoutScene!!.modelInstance.model.nodes
        val meshData: MutableMap<Mesh, MeshData> = mutableMapOf()

        val roadRegex = Regex("road(\\d+)")

        val offsetInColorForOffset = 1
        val offsetInColorForLane = 0

        for (node in nodes) {
            for (nodePart in node.parts) {
                val meshPart = nodePart.meshPart
                val res = roadRegex.matchEntire(meshPart.id) ?: continue
                val roadId = res.groups[1]?.value?.toLongOrNull() ?: throw Exception("Failed to parse road id??")
                if (!meshData.containsKey(meshPart.mesh)) {
                    meshData[meshPart.mesh] = MeshData(meshPart.mesh)
                }
                val data = meshData[meshPart.mesh]!!
                val vertices = data.vertices
                val indices = data.indices
                val colorAttrib = data.colorAttrib
                val heatmapAttrib = data.heatmapAttrib

                val getOffsetAndLane = { vertexIndex: Short ->
                    val offset =
                        vertices[colorAttrib.offset / 4 + vertexIndex * colorAttrib.numComponents + offsetInColorForOffset]
                    val lane =
                        vertices[colorAttrib.offset / 4 + vertexIndex * colorAttrib.numComponents + offsetInColorForLane]
                    offset to lane
                }

                for (i in 0..<meshPart.size step 3) {
                    val vertexIndexA = indices[meshPart.offset + i + 0]
                    val vertexIndexB = indices[meshPart.offset + i + 1]
                    val vertexIndexC = indices[meshPart.offset + i + 2]
                    val (offsetA, laneA) = getOffsetAndLane(vertexIndexA)
                    val (offsetB, laneB) = getOffsetAndLane(vertexIndexB)
                    val (offsetC, laneC) = getOffsetAndLane(vertexIndexC)
                    val lane = if (abs(laneA) >= abs(laneB) && abs(laneA) >= abs(laneC)) {
                        laneA.toInt()
                    } else if (abs(laneB) >= abs(laneA) && abs(laneB) >= abs(laneC)) {
                        laneB.toInt()
                    } else {
                        laneC.toInt()
                    }
                    vertices[heatmapAttrib.offset / 4 + vertexIndexA] =
                        getHeatmapValue(segmentMap, roadId, lane, offsetA).toFloat()
                    vertices[heatmapAttrib.offset / 4 + vertexIndexB] =
                        getHeatmapValue(segmentMap, roadId, lane, offsetB).toFloat()
                    vertices[heatmapAttrib.offset / 4 + vertexIndexC] =
                        getHeatmapValue(segmentMap, roadId, lane, offsetC).toFloat()
                }
            }
        }
        for ((mesh, data) in meshData) {
            (mesh as RoadMesh).updateVerticesImmediately(data.heatmapAttrib.offset / 4, data.vertices, mesh.numVertices)
        }
    }

    // Valid heatmap values lie in range [1.0, 2.0], offset by 1 from [0.0, 1.0]
    // heatmap value of 0.0 is an invalid value
    private fun getHeatmapValue(segments: Map<Pair<Long, Int>, ISimulation.SegmentDTO>, roadId: Long, laneId: Int, offset: Float): Double {
        val segment = segments[roadId to laneId] ?: return 0.0
        return segment.segments[min(floor(offset / segment.segmentLen).toInt(), segment.segments.size - 1)] + 1.0
    }

    fun updateLayout(layout: Layout) {
        this.layout = layout
        if (layoutScene != null) {
            sceneManager.removeScene(layoutScene)
            layoutScene!!.modelInstance.model.dispose()
        }
        layoutScene = Scene(ModelGenerator.createLayoutModel(layout))
        sceneManager.addScene(layoutScene)

        placeTrafficLights()
        placeBuildings()
    }

    private fun selectCar(vehicle: Vehicle, select: Boolean) {
        val carInstance = carInstances[vehicle.id] ?: return
        for (material in carInstance.modelInstance.materials) {
            material.set(PBRColorAttribute.createEmissive(Color.WHITE))
            material.set(
                PBRFloatAttribute.createEmissiveIntensity(if (select) { 0.2f } else { 0.0f })
            )
        }
    }

    fun updateSelectedItem(item: Any?) {
        // TODO: support selecting roads and intersections
        //  We could create separate meshes for each road and intersection
        //  And then manipulate material. Or we could do something else... Maybe in attribute?
        if (selectedItem is Vehicle) {
            selectCar(selectedItem as Vehicle, false)
        }

        selectedItem = item
        if (item == null) return

        if (item is Vehicle) {
            selectCar(item, true)
        }
    }

    fun onResize(width: Int, height: Int) {
        sceneManager.updateViewport(width.toFloat(), height.toFloat())
    }

    fun dispose() {
        for (carModel in carModels) {
            carModel.dispose()
        }
        buildingModel.dispose()
        trafficLightModel.dispose()
    }

    private fun placeTrafficLights() {
        val visited = mutableSetOf<Pair<Road, Boolean>>()
        for ((id, intersection) in layout.intersections) {
            if (!intersection.hasSignals) {
                continue
            }
            for (road in intersection.incomingRoads) {
                val isStart = road.startIntersection == intersection
                val distFromStart = if (isStart) { 0.0 } else { road.length }
                val key = road to isStart
                visited.add(key)
                if (!trafficLights.containsKey(key)) {
                    val trafficLight = Scene(trafficLightModel)
                    trafficLights[key] = trafficLight
                    sceneManager.addScene(trafficLights[key])
                }
                val scene = trafficLights[key]!!
                val centerPos = road.getPoint(distFromStart)
                val roadDirection = road.getDirection(distFromStart)
                val laneId = if (isStart) { -road.leftLane } else { road.rightLane }.toDouble()
                val offsetLen = if (isStart) {
                    road.leftLane
                } else {
                    road.rightLane
                }.toDouble() * LANE_WIDTH + LANE_WIDTH / 2
                val offset = roadDirection.cross(Vec3.UP).normalized() * laneId.sign * offsetLen
                val position = centerPos + offset
                val targetPos = centerPos + roadDirection * laneId.sign
                val newTransform = Matrix4()
                    .setTranslation(position.toGdxVec())
                    .rotateTowardTarget(targetPos.toGdxVec(), Vec3.UP.toGdxVec())
                scene.modelInstance.transform.set(newTransform)
            }
        }

        val toRemove = mutableSetOf<Pair<Road, Boolean>>()
        for ((key, scene) in trafficLights) {
            if (!visited.contains(key)) {
                toRemove.add(key)
                sceneManager.removeScene(scene)
            }
        }
        for (key in toRemove) {
            trafficLights.remove(key)
        }
    }

    private fun placeBuildings() {
        buildingScenes.forEach { sceneManager.removeScene(it) }
        buildingScenes.clear()

        layout.intersections.values.forEach { intersection ->
            if (intersection.isBuilding) {
                val road = intersection.incomingRoads.first()
                val direction = road.getDirection(road.length).normalized()
                val angle = atan2(direction.z, direction.x).toFloat()

                val buildingScene = Scene(buildingSettings[intersection.building!!.type]!!.first)
                val buildingSize = buildingSettings[intersection.building!!.type]!!.second
                val bbox = BoundingBox()
                buildingScene.modelInstance.calculateBoundingBox(bbox)
                val modelHeight = bbox.height
                val yOffset = modelHeight * buildingSize / 2.0

                val center = intersection.position.toVec3()
                    .plus(Vec3(0.0, yOffset * buildingSettings[intersection.building!!.type]!!.third, 0.0))
                buildingScene.modelInstance.transform
                    .setToTranslation(center.toGdxVec())
                    .rotate(0f, 1f, 0f, (Math.toDegrees(-angle.toDouble()) - 90).toFloat())
                    .scale(buildingSize, buildingSize, buildingSize)

                sceneManager.addScene(buildingScene)
                buildingScenes.add(buildingScene)
            }
        }
    }

    private fun turnOffHeatmap() {
        for (mesh in layoutScene!!.modelInstance.model.meshes) {
            if (mesh !is RoadMesh) {
                continue
            }
            val vertices = FloatArray(mesh.numVertices * mesh.vertexSize / 4)
            mesh.getVertices(vertices)
            val attributes = mesh.vertexAttributes
            val heatmapAttrib = attributes.findByUsage(VertexAttributes.Usage.Generic)
            for (i in 0..<mesh.numVertices) {
                vertices[heatmapAttrib.offset / 4 + i] = 0.0f
            }
            mesh.updateVertices(0, vertices)
        }
    }
}
