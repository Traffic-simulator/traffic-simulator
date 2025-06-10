package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Road

class SplitRoadStateChange(
    private var addRoadStateChange: AddRoadStateChange,
    private val originalRoad: Road,
    private val newRoads: Pair<Road, Road>,
) : IStateChange {

    private lateinit var oldIntersections: Pair<Intersection, Intersection>

    override fun apply(layout: Layout) {
        addRoadStateChange.apply(layout)
        oldIntersections = Pair(originalRoad.startIntersection, originalRoad.endIntersection)
        layout.deleteRoad(originalRoad, false)
        layout.addRoad(newRoads.first)
        layout.addRoad(newRoads.second)
    }

    override fun revert(layout: Layout) {
        layout.deleteRoad(newRoads.first)
        layout.deleteRoad(newRoads.second)
        layout.addRoad(originalRoad)
        if (!layout.intersections.contains(oldIntersections.first.id)) {
            layout.pushIntersection(oldIntersections.first)
        }
        if (!layout.intersections.contains(oldIntersections.second.id)) {
            layout.pushIntersection(oldIntersections.second)
        }
        addRoadStateChange.revert(layout)

    }

    override fun isStructuralChange(): Boolean = true
}
