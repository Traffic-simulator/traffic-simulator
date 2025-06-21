package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Road

class ConnectRoadsChange(
    private var addRoadStateChange: AddRoadStateChange,
    private val originalRoad: Road,
    private val originalRoad2: Road,
    private val clickPoint: Vec3,
    private val clickPoint2: Vec3,
) : IStateChange {

    private lateinit var splitChange: SplitRoadStateChange
    private lateinit var splitChange2: SplitRoadStateChange
    private lateinit var newRoadIntersection: Pair<Intersection, Intersection>

    override fun apply(layout: Layout) {
        newRoadIntersection = addRoadStateChange.apply(layout)

        splitChange = SplitRoadStateChange(null, originalRoad, clickPoint, false, newRoadIntersection.first)
        splitChange2 = SplitRoadStateChange(null, originalRoad2, clickPoint2, true, newRoadIntersection.second)

        splitChange.apply(layout)
        splitChange2.apply(layout)

    }

    override fun revert(layout: Layout) {
        splitChange2.revert(layout)
        splitChange.revert(layout)

        addRoadStateChange.revert(layout)
    }

    override fun isStructuralChange(): Boolean = true
}
