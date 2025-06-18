package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout.Companion.DEFAULT_INTERSECTION_PADDING
import ru.nsu.trafficsimulator.model.Road

class SplitRoadStateChange(
    private var addRoadStateChange: AddRoadStateChange?,
    private val originalRoad: Road,
    private val clickPoint: Vec3,
    private val secondRoadSplit: SplitRoadStateChange? = null
) : IStateChange {

    private lateinit var newRoad1: Road
    private lateinit var newRoad2: Road
    private lateinit var newIntersection: Intersection
    private lateinit var oldIntersections: Pair<Intersection, Intersection>

    override fun apply(layout: Layout) {
        addRoadStateChange?.apply(layout)
        val (road1, road2, intersection) = splitRoad(layout)
        this.newRoad1 = road1
        this.newRoad2 = road2
        this.newIntersection = intersection
        this.oldIntersections = Pair(originalRoad.startIntersection, originalRoad.endIntersection)
        layout.deleteRoad(originalRoad, false)
        layout.addRoad(newRoad1)
        layout.addRoad(newRoad2)
        secondRoadSplit?.apply(layout)
    }

    override fun revert(layout: Layout) {
        layout.deleteRoad(newRoad1)
        layout.deleteRoad(newRoad2)
        if (!layout.intersections.containsKey(originalRoad.startIntersection.id)) {
            layout.pushIntersection(originalRoad.startIntersection)
        }
        if (!layout.intersections.containsKey(originalRoad.endIntersection.id)) {
            layout.pushIntersection(originalRoad.endIntersection)
        }
        layout.addRoad(originalRoad)
        if (!layout.intersections.containsKey(oldIntersections.first.id)) {
            layout.pushIntersection(oldIntersections.first)
        }
        if (!layout.intersections.containsKey(oldIntersections.second.id)) {
            layout.pushIntersection(oldIntersections.second)
        }
        addRoadStateChange?.revert(layout)
        secondRoadSplit?.revert(layout)
    }

    private fun splitRoad(layout: Layout): Triple<Road, Road, Intersection> {
        val (closestPoint, _, splitGlobal) = originalRoad.geometry.closestPointWithDistance(clickPoint.xzProjection())

        val newIntersection = layout.addIntersection(closestPoint.toVec3(), null)

        val firstSpline = originalRoad.geometry.copy(
            startPadding = originalRoad.startPadding - DEFAULT_INTERSECTION_PADDING,
            endPadding = originalRoad.geometry.length - splitGlobal
        )

        val secondSpline = originalRoad.geometry.copy(
            startPadding = splitGlobal,
            endPadding = originalRoad.endPadding - DEFAULT_INTERSECTION_PADDING
        )

        val road1 = Road(
            id = layout.roadIdCount++,
            startIntersection = originalRoad.startIntersection,
            endIntersection = newIntersection,
            geometry = firstSpline,
        )

        val road2 = Road(
            id = layout.roadIdCount++,
            startIntersection = newIntersection,
            endIntersection = originalRoad.endIntersection,
            geometry = secondSpline,
        )

        return Triple(road1, road2, newIntersection)
    }

    override fun isStructuralChange(): Boolean = true
}
