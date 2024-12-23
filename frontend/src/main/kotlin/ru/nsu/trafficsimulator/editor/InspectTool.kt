package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.Input
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
import ru.nsu.trafficsimulator.model.*

class InspectTool : IEditingTool {
    private val name = "Inspect"
    private var draggingIntersection: Intersection? = null
    private var layout: Layout? = null
    private var camera: Camera? = null
    private val spheres = mutableMapOf<Long, ModelInstance>()

    private var selectedRoad: Road? = null
    private var draggingDirectionIsStart: Boolean? = null
    private val directionSpheres = mutableListOf<ModelInstance>()
    private var draggingDirectionSphere: ModelInstance? = null
    private val curveCoeff: Double = 4.0

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false

        draggingIntersection = findRoadIntersectionAt(intersection)
        if (draggingIntersection != null) {
            draggingDirectionSphere = null
            return true
        }

        if (selectedRoad != null) {
            draggingDirectionSphere = findDirectionSphere(intersection)
            if (draggingDirectionSphere != null) {
                return true
            }
        }
        selectedRoad = findRoad(layout!!, intersection)
        if (selectedRoad != null) {
            directionSpheres[0].transform.setToTranslation(
                (selectedRoad!!.startIntersection!!.position - selectedRoad!!.getDirection(0.0) / curveCoeff).toGdxVec()
            )
            directionSpheres[1].transform.setToTranslation(
                (selectedRoad!!.endIntersection!!.position + selectedRoad!!.getDirection(selectedRoad!!.length) / curveCoeff).toGdxVec()
            )
        }
        return true
    }

    private fun applyRoadDirections(): IStateChange? {
        if (draggingDirectionSphere == null) return null

        val editedRoad = selectedRoad ?: return null

        val startOffset = Vec3(directionSpheres[0].transform.getTranslation(Vector3()))
        val startDir = (selectedRoad!!.startIntersection!!.position - startOffset) * curveCoeff
        val endOffset = Vec3(directionSpheres[1].transform.getTranslation(Vector3()))
        val endDir = (endOffset - editedRoad.endIntersection!!.position) * curveCoeff
        return RedirectRoadStateChange(editedRoad, editedRoad.startIntersection!!.position + startDir, editedRoad.endIntersection!!.position + endDir)
    }

    private fun applyIntersectionPosition(): IStateChange? {
        if (draggingIntersection == null) return null

        if (selectedRoad != null) {
            val startPos = selectedRoad!!.startIntersection!!.position
            val endPos = selectedRoad!!.endIntersection!!.position
            directionSpheres[0].transform.setToTranslation(
                (startPos - selectedRoad!!.getDirection(0.0) / curveCoeff).toGdxVec()
            )
            directionSpheres[1].transform.setToTranslation(
                (endPos + selectedRoad!!.getDirection(selectedRoad!!.length) / curveCoeff).toGdxVec()
            )
        }
        val res = MoveIntersectionStateChange(draggingIntersection!!, Vec3(spheres[draggingIntersection!!.id]!!.transform.getTranslation(Vector3())))
        draggingIntersection = null
        return res
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        if (button != Input.Buttons.LEFT) return null
        applyRoadDirections()?.let {return it}
        applyIntersectionPosition()?.let {return it}
        return null
    }

    override fun handleDrag(screenPos: Vec2) {
        if (draggingIntersection == null && draggingDirectionSphere == null) return
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return

        if (draggingIntersection != null) {
            spheres[draggingIntersection!!.id]?.transform?.setToTranslation(intersection)
            if (selectedRoad != null) {
                val startPos = if (selectedRoad!!.startIntersection!! == draggingIntersection) { Vec3(intersection) } else { selectedRoad!!.startIntersection!!.position }
                val endPos = if (selectedRoad!!.endIntersection!! == draggingIntersection) { Vec3(intersection) } else { selectedRoad!!.endIntersection!!.position }
                directionSpheres[0].transform.setToTranslation(
                    (startPos - selectedRoad!!.getDirection(0.0) / curveCoeff).toGdxVec()
                )
                directionSpheres[1].transform.setToTranslation(
                    (endPos + selectedRoad!!.getDirection(selectedRoad!!.length) / curveCoeff).toGdxVec()
                )
            }
        } else {
            draggingDirectionSphere!!.transform.setToTranslation(intersection)
        }
    }

    override fun render(modelBatch: ModelBatch?) {
        for ((_, sphere) in spheres) {
            modelBatch?.render(sphere)
        }
        if (selectedRoad != null) {
            modelBatch?.render(directionSpheres[0])
            modelBatch?.render(directionSpheres[1])
        }
    }

    override fun init(layout: Layout, camera: Camera) {
        this.layout = layout
        this.camera = camera
        this.spheres.clear()
        val model = createSphere()
        for ((id, intersection) in layout.intersections) {
            spheres[id] = ModelInstance(model)
            spheres[id]!!.transform.setToTranslation(intersection.position.toGdxVec())
        }

        val (left, right) = createDirectionPairSphere()
        directionSpheres.clear()
        directionSpheres.add(ModelInstance(left))
        directionSpheres.add(ModelInstance(right))
        selectedRoad = null
        draggingDirectionSphere = null
    }

    private fun findRoadIntersectionAt(point: Vector3): Intersection? {
        for ((_, intersection) in layout!!.intersections) {
            if (intersection.position.distance(Vec3(point)) < 5.0f) {
                return intersection
            }
        }
        return null
    }

    private fun createSphere(): Model? {
        val modelBuilder = ModelBuilder()
        val material = Material(ColorAttribute.createDiffuse(Color.RED))
        val sphere = modelBuilder.createSphere(
            5.0f,
            5.0f,
            5.0f,
            10,
            10,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        return sphere
    }

    private fun findDirectionSphere(intersection: Vector3): ModelInstance? {
        if (directionSpheres[0].transform.getTranslation(Vector3()).dst(intersection) < 5.0f) {
            draggingDirectionIsStart = true
            return directionSpheres[0]
        }
        if (directionSpheres[1].transform.getTranslation(Vector3()).dst(intersection) < 5.0f) {
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
