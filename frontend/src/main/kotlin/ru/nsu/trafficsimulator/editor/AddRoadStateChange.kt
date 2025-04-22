package ru.nsu.trafficsimulator.editor

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.findRoadIntersectionAt
import ru.nsu.trafficsimulator.model.*

class AddRoadStateChange(layout: Layout, private val startPos: Vec3, private val startDir: Vec3, private val endPos: Vec3, private val endDir: Vec3) : IStateChange {
    private val start: Intersection? = findRoadIntersectionAt(layout, startPos)
    private val end: Intersection? = findRoadIntersectionAt(layout, endPos)

    private var newStart: Intersection? = null
    private var newEnd: Intersection? = null
    private var newRoad: Road? = null
    override fun apply(layout: Layout) {
        val from = if (start != null) {
            start
        } else {
            if (newStart == null) {
                newStart = layout.addIntersection(startPos)
            }
            newStart!!
        }
        val to = if (end != null) {
            end
        } else {
            if (newEnd == null) {
                newEnd = layout.addIntersection(endPos)
            }
            newEnd!!
        }
        if (!layout.intersections.contains(from.id)) {
            layout.intersections[from.id] = from
        }
        if (!layout.intersections.contains(to.id)) {
            layout.intersections[to.id] = to
        }
        newRoad = layout.addRoad(from, startDir, to, endDir)
    }

    override fun revert(layout: Layout) {
        newRoad?.let {layout.deleteRoad(it)}
    }
}
