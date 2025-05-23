package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.IntersectionRoad
import ru.nsu.trafficsimulator.model.Road

class AddRoadStateChange(private val start: Intersection, private val startDir: Vec3, private val end: Intersection, private val endDir: Vec3) : IStateChange {
    private var affectedIntersectionRoads: List<IntersectionRoad> = emptyList()
    private var newRoad: Road? = null

    override fun apply(layout: Layout) {
        affectedIntersectionRoads = start.intersectionRoads.values.toList() + end.intersectionRoads.values.toList()
        if (!layout.intersections.contains(start.id))
            layout.intersections[start.id] = start
        if (!layout.intersections.contains(end.id))
            layout.intersections[end.id] = end
        if (newRoad == null) {
            newRoad = layout.addRoad(start, startDir, end, endDir)
        } else {
            layout.addRoad(newRoad!!)
        }
    }

    override fun revert(layout: Layout) {
        if (newRoad != null) {
            layout.deleteRoad(newRoad!!)
        }
    }
}
