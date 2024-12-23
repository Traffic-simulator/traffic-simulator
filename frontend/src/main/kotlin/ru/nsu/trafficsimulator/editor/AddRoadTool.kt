package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.model.*

class AddRoadTool : IEditingTool {
    private val name = "Add Road"
    private var layout: Layout? = null
    private var camera: Camera? = null
    private val startDirectionLength = 25.0

    private val selectedIntersections = arrayOfNulls<Intersection>(2)
    private var selectedIntersectionCount = 0
    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return true
        val intersectionPoint = getIntersectionWithGround(screenPos, camera!!) ?: return true

        var roadIntersection = findRoadIntersectionAt(intersectionPoint)
        if (roadIntersection == null) {
            roadIntersection = layout!!.addIntersection(Vec3(intersectionPoint))
        }
        selectedIntersections[selectedIntersectionCount] = roadIntersection

        selectedIntersectionCount += 1
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): Boolean {
        if (selectedIntersectionCount != 2) return false
        selectedIntersectionCount = 0
        if (selectedIntersections[0] == selectedIntersections[1]) return false

        val dir = (selectedIntersections[1]!!.position - selectedIntersections[0]!!.position).normalized()
        val startDirection = selectedIntersections[0]!!.position + dir * startDirectionLength
        val endDirection = selectedIntersections[1]!!.position + dir * startDirectionLength

        layout!!.addRoad(
            selectedIntersections[0]!!, startDirection, selectedIntersections[1]!!, endDirection
        )
        return true
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch?) {
        return
    }

    override fun init(layout: Layout, camera: Camera) {
        this.layout = layout
        this.camera = camera
    }

    private fun findRoadIntersectionAt(point: Vector3): Intersection? {
        for ((_, intersection) in layout!!.intersections) {
            if (intersection.position.distance(Vec3(point)) < 5.0f) {
                return intersection
            }
        }
        return null
    }
}
