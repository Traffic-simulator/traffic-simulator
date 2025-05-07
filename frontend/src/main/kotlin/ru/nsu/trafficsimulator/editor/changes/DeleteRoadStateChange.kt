package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.IntersectionRoad
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class DeleteRoadStateChange(private val road: Road) : IStateChange {
    private val start = road.startIntersection
    private val end = road.endIntersection
    private val startSignal = start.signals[road]
    private val endSignal = end.signals[road]

    private val roadConnections = mutableListOf<Pair<Intersection, IntersectionRoad>>()

    init {
        start.intersectionRoads.forEach { ir ->
            if (ir.fromRoad == road || ir.toRoad == road) {
                roadConnections.add(start to ir)
            }
        }
        end.intersectionRoads.forEach { ir ->
            if (ir.fromRoad == road || ir.toRoad == road) {
                roadConnections.add(end to ir)
            }
        }
    }

    override fun apply(layout: Layout) {
        layout.roads.values.firstOrNull {
            it.id == road.id
        }?.let { roadToDelete ->
            layout.deleteRoad(roadToDelete)
        }
    }

    override fun revert(layout: Layout) {
        if (!layout.intersections.contains(start.id)) layout.intersections[start.id] = start
        if (!layout.intersections.contains(end.id)) layout.intersections[end.id] = end

        layout.addRoad(road)

        roadConnections.forEach { (intersection, originalIr) ->
            intersection.intersectionRoads.add(originalIr)
        }

        if (startSignal != null) {
            start.signals[road] = startSignal
        }
        if (endSignal != null) {
            end.signals[road] = endSignal
        }
    }
}
