package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Vec2
import ru.nsu.trafficsimulator.model.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.*

class EditRoadTool : IEditingTool {
    private val name = "Edit Road"
    private var layout: Layout? = null
    private var camera: Camera? = null

    private var selectedRoad: Road? = null
    private var draggingDirectionIsStart: Boolean? = null
    private val directionSpheres = mutableListOf<ModelInstance>()
    private var draggingDirectionSphere: ModelInstance? = null
    private val curveCoeff: Double = 4.0

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return true
        if (selectedRoad != null) {
            draggingDirectionSphere = findDirectionSphere(intersection)
            if (draggingDirectionSphere != null) {
                return true
            }
        }
        val road = findRoad(layout!!, intersection)
        selectedRoad = road
        if (road != null) {
            directionSpheres[0].transform.setToTranslation((road.startIntersection!!.position - road.getDirection(0.0) / curveCoeff).toGdxVec())
            directionSpheres[1].transform.setToTranslation((road.endIntersection!!.position + road.getDirection(road.length) / curveCoeff).toGdxVec())
        }
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): Boolean {
        if (draggingDirectionSphere == null) return false

        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false
        draggingDirectionSphere!!.transform.setToTranslation(intersection)
        val editedRoad = selectedRoad ?: return false

        val direction = Vector3()
        draggingDirectionSphere?.transform?.getTranslation(direction)
        val directionVec = Vec3(direction)
        if (draggingDirectionIsStart == true) {
            val t = (selectedRoad!!.startIntersection!!.position - directionVec) * curveCoeff
            editedRoad.redirectRoad(
                editedRoad.startIntersection!!,
                editedRoad.startIntersection!!.position + t
            )
        } else {
            val t = (directionVec - editedRoad.endIntersection!!.position) * curveCoeff
            editedRoad.redirectRoad(
                editedRoad.endIntersection!!,
                editedRoad.endIntersection!!.position + t
            )
        }
        return true
    }

    override fun handleDrag(screenPos: Vec2) {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return
        if (draggingDirectionSphere == null) return

        draggingDirectionSphere!!.transform.setToTranslation(intersection)
    }

    override fun render(modelBatch: ModelBatch?) {
        if (selectedRoad != null) {
            modelBatch?.render(directionSpheres[0])
            modelBatch?.render(directionSpheres[1])
        }
    }

    override fun init(layout: Layout, camera: Camera) {
        this.camera = camera
        this.layout = layout
        val (left, right) = createDirectionPairSphere()
        directionSpheres.clear()
        directionSpheres.add(ModelInstance(left))
        directionSpheres.add(ModelInstance(right))
        selectedRoad = null
        draggingDirectionSphere = null
    }

    private fun findDirectionSphere(intersection: Vector3): ModelInstance? {
        if (directionSpheres[0].transform.getTranslation(Vector3())
                .dst(intersection) < 5.0f
        ) {
            draggingDirectionIsStart = true
            return directionSpheres[0]
        }
        if (directionSpheres[1].transform.getTranslation(Vector3())
                .dst(intersection) < 5.0f
        ) {
            draggingDirectionIsStart = false
            return directionSpheres[1]
        }
        draggingDirectionIsStart = null
        return null
    }

    private fun createDirectionPairSphere(): Pair<Model, Model> {
        val modelBuilder = ModelBuilder()

        val startMaterial = Material(ColorAttribute.createDiffuse(Color.BLUE))
        val start = modelBuilder.createSphere(
            5.0f,
            5.0f,
            5.0f,
            10,
            10,
            startMaterial,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        val endMaterial = Material(ColorAttribute.createDiffuse(Color.SKY))
        val end = modelBuilder.createSphere(
            5.0f,
            5.0f,
            5.0f,
            10,
            10,
            endMaterial,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        return Pair(start, end)
    }
}
