package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.IntersectionRoad

class AddRoadStateChange(private val start: Intersection, private val startDir: Vec3, private val end: Intersection, private val endDir: Vec3) : IStateChange {
    private var affectedIntersectionRoads: List<IntersectionRoad> = emptyList()

    override fun apply(layout: Layout) {
        affectedIntersectionRoads = start.intersectionRoads.toList() + end.intersectionRoads.toList()
        if (!layout.intersections.contains(start.id))
            layout.intersections[start.id] = start
        if (!layout.intersections.contains(end.id))
            layout.intersections[end.id] = end
        layout.addRoad(start, startDir, end, endDir)
    }

    override fun revert(layout: Layout) {
        layout.roads.values.firstOrNull {
            it.startIntersection == start && it.endIntersection == end
        }?.let { roadToDelete ->
            // 1. Удаляем все IntersectionRoad, связанные с этой дорогой
            start.intersectionRoads.removeIf { ir ->
                ir.fromRoad == roadToDelete || ir.toRoad == roadToDelete
            }
            end.intersectionRoads.removeIf { ir ->
                ir.fromRoad == roadToDelete || ir.toRoad == roadToDelete
            }

            // 2. Удаляем саму дорогу
            layout.deleteRoad(roadToDelete)
        }
    }
}
