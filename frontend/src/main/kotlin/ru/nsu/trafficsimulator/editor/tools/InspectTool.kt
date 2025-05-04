package ru.nsu.trafficsimulator.editor.tools

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
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.changes.MoveIntersectionStateChange
import ru.nsu.trafficsimulator.editor.changes.RedirectRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.editor.*

class InspectTool : IEditingTool {
    private val name = "Edit"
    private var draggingIntersection: Intersection? = null
    private val sphereForDraggingIntersection = ModelInstance(createSphere(Color.RED))
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
        if (button != Input.Buttons.LEFT) return false
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false

        draggingIntersection = findRoadIntersectionAt(intersection)
        if (draggingIntersection != null) {
            sphereForDraggingIntersection.transform?.setToTranslation(intersection.toGdxVec())
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
                (selectedRoad!!.startIntersection.position.toVec3() - selectedRoad!!.getDirection(0.0) / curveCoeff).toGdxVec()
            )
            directionSpheres[1].transform.setToTranslation(
                (selectedRoad!!.endIntersection.position.toVec3() + selectedRoad!!.getDirection(selectedRoad!!.length) / curveCoeff).toGdxVec()
            )
        }
        return draggingDirectionSphere != null || draggingIntersection != null
    }

    private fun applyRoadDirections(): IStateChange? {
        if (draggingDirectionSphere == null) return null

        val editedRoad = selectedRoad ?: return null

        val startOffset = Vec3(directionSpheres[0].transform.getTranslation(Vector3()))
        val startDir = (selectedRoad!!.startIntersection.position.toVec3() - startOffset) * curveCoeff
        val endOffset = Vec3(directionSpheres[1].transform.getTranslation(Vector3()))
        val endDir = (endOffset - editedRoad.endIntersection.position.toVec3()) * curveCoeff
        draggingDirectionSphere = null
        return RedirectRoadStateChange(
            editedRoad,
            editedRoad.startIntersection.position.toVec3() + startDir,
            editedRoad.endIntersection.position.toVec3() + endDir
        )
    }

    private fun applyIntersectionPosition(): IStateChange? {
        if (draggingIntersection == null) return null

        if (selectedRoad != null) {
            val startPos = selectedRoad!!.startIntersection.position.toVec3()
            val endPos = selectedRoad!!.endIntersection.position.toVec3()
            directionSpheres[0].transform.setToTranslation(
                (startPos - selectedRoad!!.getDirection(0.0) / curveCoeff).toGdxVec()
            )
            directionSpheres[1].transform.setToTranslation(
                (endPos + selectedRoad!!.getDirection(selectedRoad!!.length) / curveCoeff).toGdxVec()
            )
        }
        val res = MoveIntersectionStateChange(draggingIntersection!!, Vec3(sphereForDraggingIntersection.transform.getTranslation(Vector3())))
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
            sphereForDraggingIntersection.transform?.setToTranslation(intersection.toGdxVec())
            if (selectedRoad != null) {
                val startPos = if (selectedRoad!!.startIntersection == draggingIntersection) {
                    intersection
                } else {
                    selectedRoad!!.startIntersection.position.toVec3()
                }
                val endPos = if (selectedRoad!!.endIntersection == draggingIntersection) {
                    intersection
                } else {
                    selectedRoad!!.endIntersection.position.toVec3()
                }
                directionSpheres[0].transform.setToTranslation(
                    (startPos - selectedRoad!!.getDirection(0.0) / curveCoeff).toGdxVec()
                )
                directionSpheres[1].transform.setToTranslation(
                    (endPos + selectedRoad!!.getDirection(selectedRoad!!.length) / curveCoeff).toGdxVec()
                )
            }
        } else {
            draggingDirectionSphere!!.transform.setToTranslation(intersection.toGdxVec())
        }
    }

    override fun render(modelBatch: ModelBatch?) {
        if (draggingIntersection != null) {
            modelBatch?.render(sphereForDraggingIntersection)
        }

        if (selectedRoad != null) {
            modelBatch?.render(directionSpheres[0])
            modelBatch?.render(directionSpheres[1])
        }
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.layout = layout
        this.camera = camera

        if (reset) {
            val (left, right) = createDirectionPairSphere()
            directionSpheres.clear()
            directionSpheres.add(ModelInstance(left))
            directionSpheres.add(ModelInstance(right))
            selectedRoad = null
            draggingDirectionSphere = null
        }
    }

    private fun findRoadIntersectionAt(point: Vec3): Intersection? {
        for ((_, intersection) in layout!!.intersections) {
            if (intersection.position.distance(point.xzProjection()) < 5.0f) {
                return intersection
            }
        }
        return null
    }

    private fun findDirectionSphere(intersection: Vec3): ModelInstance? {
        if (directionSpheres[0].transform.getTranslation(Vector3()).dst(intersection.toGdxVec()) < 5.0f) {
            draggingDirectionIsStart = true
            return directionSpheres[0]
        }
        if (directionSpheres[1].transform.getTranslation(Vector3()).dst(intersection.toGdxVec()) < 5.0f) {
            draggingDirectionIsStart = false
            return directionSpheres[1]
        }
        draggingDirectionIsStart = null
        return null
    }

    private fun createDirectionPairSphere(): Pair<Model, Model> {
        return Pair(createSphere(Color.BLUE), createSphere(Color.SKY))
    }
}
