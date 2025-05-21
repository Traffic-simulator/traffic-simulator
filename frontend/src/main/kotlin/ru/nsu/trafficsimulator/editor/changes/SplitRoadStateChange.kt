package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Road

class SplitRoadStateChange(
    private val originalRoad: Road,
    private val newRoads: Pair<Road, Road>,
    private val newIntersection: Intersection
) : IStateChange {

    private lateinit var oldIntersections: Pair<Intersection, Intersection>

    override fun apply(layout: Layout) {
        oldIntersections = Pair(originalRoad.startIntersection, originalRoad.endIntersection)
        layout.deleteRoad(originalRoad)
        layout.addIntersection(newIntersection.position.toVec3())
        layout.addRoad(newRoads.first)
        layout.addRoad(newRoads.second)
    }

    override fun revert(layout: Layout) {
        layout.deleteRoad(newRoads.first)
        layout.deleteRoad(newRoads.second)
        layout.deleteIntersection(newIntersection)
        layout.pushIntersection(oldIntersections.first)
        layout.pushIntersection(oldIntersections.second)
        layout.addRoad(originalRoad)
    }

    override fun isStructuralChange(): Boolean = true
}
