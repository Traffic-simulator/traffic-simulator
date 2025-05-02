package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
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
        if (button != Input.Buttons.LEFT) return false
        val intersectionPoint = getIntersectionWithGround(screenPos, camera!!) ?: return false

        var roadIntersection = findRoadIntersectionAt(intersectionPoint)
        if (roadIntersection == null) {
            roadIntersection = layout!!.addIntersection(Vec3(intersectionPoint))
        }
        selectedIntersections[selectedIntersectionCount] = roadIntersection

        selectedIntersectionCount += 1
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        if (selectedIntersectionCount != 2) return null
        selectedIntersectionCount = 0
        if (selectedIntersections[0] == selectedIntersections[1]) return null

        val dir = (selectedIntersections[1]!!.position - selectedIntersections[0]!!.position).normalized()
        val startDirection = selectedIntersections[0]!!.position + dir * startDirectionLength
        val endDirection = selectedIntersections[1]!!.position + dir * startDirectionLength

        return AddRoadStateChange(selectedIntersections[0]!!, startDirection.toVec3(), selectedIntersections[1]!!, endDirection.toVec3())
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
        selectedIntersectionCount = 0
    }

    private fun findRoadIntersectionAt(point: Vector3): Intersection? {
        for ((_, intersection) in layout!!.intersections) {
            if (intersection.position.distance(Vec3(point).xzProjection()) < 5.0f) {
                return intersection
            }
        }
        return null
    }
}
