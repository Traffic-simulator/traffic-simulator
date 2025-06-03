package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.editor.Editor
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.changes.SplitRoadStateChange
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
    private var isSpitting = false

    private data class SplitData(
        val originalRoad: Road,
        val newRoad1: Road,
        val newRoad2: Road,
        val newIntersection: Intersection
    )

    private var splitData: SplitData? = null

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false
        val intersectionPoint = getIntersectionWithGround(screenPos, camera!!) ?: return false

        var targetIntersection = findRoadIntersectionAt(intersectionPoint)

        if (targetIntersection == null) {
            val closestRoad = layout!!.findClosestRoad(intersectionPoint)
            if (closestRoad != null) {
                isSpitting = true
                val (road1, road2) = layout!!.splitRoad(closestRoad, intersectionPoint)

                layout!!.roadIdCount = maxOf(layout!!.roadIdCount, road2.id + 1)

                splitData = SplitData(closestRoad, road1, road2, road1.endIntersection)

                targetIntersection = road1.endIntersection
            } else {
                targetIntersection = layout!!.addIntersection(intersectionPoint)
            }
        }
        if (targetIntersection != null) {
            if (targetIntersection.isBuilding) return false
        }
        selectedIntersections[selectedIntersectionCount] = targetIntersection

        selectedIntersections[selectedIntersectionCount] = targetIntersection
        selectedIntersectionCount++
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        if (selectedIntersectionCount != 2) return null
        selectedIntersectionCount = 0
        if (selectedIntersections[0] == selectedIntersections[1]) return null

        val dir = (selectedIntersections[1]!!.position - selectedIntersections[0]!!.position).normalized()
        val startDirection = selectedIntersections[0]!!.position + dir * startDirectionLength
        val endDirection = selectedIntersections[1]!!.position + dir * startDirectionLength


        val change = AddRoadStateChange(selectedIntersections[0]!!, startDirection.toVec3(), selectedIntersections[1]!!, endDirection.toVec3())
        if (!isSpitting){
            isSpitting = false
            return change
        } else {
            isSpitting = false
            return SplitRoadStateChange(change, splitData!!.originalRoad, Pair(splitData!!.newRoad1, splitData!!.newRoad2))
        }
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch?) {
        return
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.layout = layout
        this.camera = camera
        selectedIntersectionCount = 0
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
