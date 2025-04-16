package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.math.Vec3

class AddRoadStateChange(private val start: Intersection, private val startDir: Vec3, private val end: Intersection, private val endDir: Vec3) : IStateChange {
    override fun apply(layout: Layout) {
        layout.addRoad(start, startDir, end, endDir)
    }

    override fun revert(layout: Layout) {
        for ((_, road) in layout.roads) {
            if (road.startIntersection == start && road.endIntersection == end) {
                layout.deleteRoad(road)
                return
            }
        }
        println("Failed to find road with original intersections")
    }
}
