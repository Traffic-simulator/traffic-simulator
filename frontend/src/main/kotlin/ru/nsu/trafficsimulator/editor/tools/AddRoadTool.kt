package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.editor.changes.SplitRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

private const val START_DIRECTION_LENGTH = 25.0


class AddRoadTool : IEditingTool {
    private val name = "Add Road"
    private var layout: Layout? = null
    private var camera: Camera? = null

    private var existingStartIntersection: Intersection? = null
    private var startPosition: Vec3? = null

    private data class SplitData(
        val originalRoad: Road,
        val newRoad1: Road,
        val newRoad2: Road,
        val newIntersection: Intersection
    )

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
                val splitResults = mutableListOf<SplitData>()

                existingStartIntersection = processRoadSplit(
                    startPosition,
                    layout!!,
                    existingStartIntersection,
                    splitResults
                )

                val existingEndIntersection = processRoadSplit(
                    endPosition,
                    layout!!,
                    findRoadIntersectionAt(endPosition),
                    splitResults
                )


                val dir = (endPosition - startPosition).normalized()
                val startDirection = startPosition + dir * START_DIRECTION_LENGTH
                val endDirection = endPosition + dir * START_DIRECTION_LENGTH

                val roadChange = AddRoadStateChange(
                    startPosition to startDirection,
                    existingStartIntersection,
                    endPosition to endDirection,
                    existingEndIntersection
                )

                return when (splitResults.size) {
                    0 -> roadChange
                    1 -> SplitRoadStateChange(
                        roadChange,
                        splitResults[0].originalRoad,
                        Pair(splitResults[0].newRoad1, splitResults[0].newRoad2)
                    )
                    2 -> SplitRoadStateChange(
                        roadChange,
                        splitResults[0].originalRoad,
                        Pair(splitResults[0].newRoad1, splitResults[0].newRoad2),
                        SplitRoadStateChange(
                            null,
                            splitResults[1].originalRoad,
                            Pair(splitResults[1].newRoad1, splitResults[1].newRoad2)
                        )
                    )
                    else -> null
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

    private fun processRoadSplit(
        position: Vec3,
        layout: Layout,
        currentIntersection: Intersection?,
        splitResults: MutableList<SplitData>
    ): Intersection {
        if (currentIntersection != null) return currentIntersection

        return findRoad(layout, position)?.let { road ->
            val (road1, road2) = layout.splitRoad(road, position)
            layout.roadIdCount = maxOf(layout.roadIdCount, road2.id + 1)
            splitResults.add(SplitData(road, road1, road2, road1.endIntersection))
            road1.endIntersection
        } ?: layout.addIntersection(position, null)
    }
}
