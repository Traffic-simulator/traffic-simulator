package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
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

    private val selectedIntersections = arrayOfNulls<Intersection>(2)
    private var selectedIntersectionCount = 0
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
                val existingEndIntersection = findRoadIntersectionAt(endPosition)

                val dir = (endPosition - startPosition).normalized()
                val startDirection = startPosition + dir * START_DIRECTION_LENGTH
                val endDirection = endPosition + dir * START_DIRECTION_LENGTH

                return AddRoadStateChange(
                    startPosition to startDirection,
                    existingEndIntersection,
                    endPosition to endDirection,
                    existingEndIntersection
                )
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
