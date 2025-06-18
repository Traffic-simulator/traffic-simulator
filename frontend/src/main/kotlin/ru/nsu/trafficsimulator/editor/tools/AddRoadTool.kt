package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.ConnectRoadsChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.changes.SplitRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

private const val START_DIRECTION_LENGTH = 25.0


class AddRoadTool : IEditingTool {
    private val name = "Add Road"
    private var layout: Layout? = null
    private var camera: Camera? = null

    private var existingStartIntersection: Intersection? = null
    private var startPosition: Vec3? = null

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return false
        startPosition = getIntersectionWithGround(screenPos, camera!!) ?: return false

        getIntersectionWithGround(screenPos, camera!!)?.let { intersectionPosition ->
            startPosition = intersectionPosition

            existingStartIntersection = findRoadIntersectionAt(intersectionPosition)
            existingStartIntersection?.let {
                if (it.isBuilding) {
                    return false
                }
            }
        }
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        try {
            startPosition?.let { startPosition ->
                val endPosition = getIntersectionWithGround(screenPos, camera!!) ?: return null
                var existingEndIntersection = findRoadIntersectionAt(endPosition)

                val dir = (endPosition - startPosition).normalized()
                val startDirection = startPosition + dir * START_DIRECTION_LENGTH
                val endDirection = endPosition + dir * START_DIRECTION_LENGTH

                val roadChange = AddRoadStateChange(
                    startPosition to startDirection,
                    existingStartIntersection,
                    endPosition to endDirection,
                    existingEndIntersection
                )

                val startRoad = findRoad(layout!!, startPosition)
                val endRoad = findRoad(layout!!, endPosition)

                return when {
                    // If we are connecting 2 roads
                    startRoad != null && endRoad != null && existingStartIntersection == null && existingEndIntersection == null -> {
                        val (startPosOnRoad, _) = startRoad.geometry.closestPoint(startPosition.xzProjection())
                        existingStartIntersection = findRoadIntersectionAt(startPosOnRoad.toVec3())
                        val (endPosOnRoad, _) = endRoad.geometry.closestPoint(endPosition.xzProjection())
                        existingEndIntersection = findRoadIntersectionAt(endPosOnRoad.toVec3())
                        ConnectRoadsChange(
                            AddRoadStateChange(
                                startPosOnRoad.toVec3() to startDirection,
                                existingStartIntersection,
                                endPosOnRoad.toVec3() to endDirection,
                                existingEndIntersection
                            ),
                            startRoad, endRoad, startPosOnRoad.toVec3(), endPosOnRoad.toVec3()
                        )
                    }
                    // If we are splitting by first click
                    startRoad != null && existingStartIntersection == null -> {
                        val (startPos, _) = startRoad.geometry.closestPoint(startPosition.xzProjection())
                        existingStartIntersection = findRoadIntersectionAt(startPos.toVec3())
                        SplitRoadStateChange(
                            AddRoadStateChange(
                                startPos.toVec3() to startDirection,
                                existingStartIntersection,
                                endPosition to endDirection,
                                existingEndIntersection
                            ),
                            startRoad, startPos.toVec3(), false
                        )
                    }
                    // If we are splitting by second click
                    endRoad != null && existingEndIntersection == null -> {
                        val (endPos, _) = endRoad.geometry.closestPoint(endPosition.xzProjection())
                        existingEndIntersection = findRoadIntersectionAt(endPos.toVec3())
                        SplitRoadStateChange(
                            AddRoadStateChange(
                                startPosition to startDirection,
                                existingStartIntersection,
                                endPos.toVec3() to endDirection,
                                existingEndIntersection
                            ),
                            endRoad, endPos.toVec3(), true
                        )
                    }
                    // If we aren't splitting
                    else -> roadChange
                }
            }
            return null
        } finally {
            startPosition = null
            existingStartIntersection = null
        }
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch) {
        return
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
