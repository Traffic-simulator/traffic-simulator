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
        addRoadStateChange.revert(layout)
        layout.deleteRoad(newRoads.first)
        layout.deleteRoad(newRoads.second)
        layout.pushIntersection(oldIntersections.first)
        layout.pushIntersection(oldIntersections.second)
        layout.addRoad(originalRoad)
    }

    override fun isStructuralChange(): Boolean = true
}
