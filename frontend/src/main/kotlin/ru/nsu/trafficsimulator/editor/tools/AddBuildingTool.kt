package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import ru.nsu.trafficsimulator.editor.changes.AddBuildingStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class AddBuildingTool : IEditingTool {
    private val name = "Add Building"
    private var layout: Layout? = null
    private var camera: Camera? = null
    private val startDirectionLength = 25.0

    private var start: Intersection? = null;
    private lateinit var end: Vec3;
    private var selectedIntersectionCount = 0

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false
        val intersectionPoint = getIntersectionWithGround(screenPos, camera!!) ?: return false

        if (selectedIntersectionCount == 0) {
            start = findRoadIntersectionAt(intersectionPoint) ?: return false
        } else {
            val roadIntersection = findRoadIntersectionAt(intersectionPoint)
            if (roadIntersection != null) return false
            end = intersectionPoint;
        }

        selectedIntersectionCount += 1
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        if (selectedIntersectionCount != 2) return null
        selectedIntersectionCount = 0
        if (start!!.position.toVec3() == end) return null

        val dir = (end.xzProjection() - start!!.position).normalized()
        val startDirection = start!!.position + dir * startDirectionLength
        val endDirection = end.xzProjection() + dir * startDirectionLength

        return AddBuildingStateChange(
            start!!,
            startDirection.toVec3(),
            end,
            endDirection.toVec3()
        )
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.layout = layout
        this.camera = camera
    }

    private fun findRoadIntersectionAt(point: Vec3): Intersection? {
        for ((_, intersection) in layout!!.intersections) {
            if (intersection.position.distance(point.xzProjection()) < 5.0f) {
                return intersection
            }
        }
        return null
    }
}
