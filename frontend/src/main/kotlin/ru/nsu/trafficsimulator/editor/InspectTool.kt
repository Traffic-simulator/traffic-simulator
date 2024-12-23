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
import ru.nsu.trafficsimulator.editor.Editor.Companion
import ru.nsu.trafficsimulator.model.*

class InspectTool : IEditingTool {
    private val name = "Inspect"
    private var draggingIntersection: Intersection? = null
    private var layout: Layout? = null
    private var camera: Camera? = null
    private val spheres = mutableMapOf<Long, ModelInstance>()

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false

        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false

        draggingIntersection = findRoadIntersectionAt(intersection)
        return draggingIntersection != null
    }

    override fun handleUp(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false
        val toChange = draggingIntersection != null
        draggingIntersection = null
        return toChange
    }

    override fun handleDrag(screenPos: Vec2) {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return
        if (draggingIntersection == null) return

        layout!!.moveIntersection(draggingIntersection!!, Vec3(intersection))
        spheres[draggingIntersection!!.id]?.transform?.setToTranslation(intersection)
    }

    override fun render(modelBatch: ModelBatch?) {
        for ((_, sphere) in spheres) {
            modelBatch?.render(sphere)
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
}
