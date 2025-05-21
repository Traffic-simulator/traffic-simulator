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
        start.intersectionRoads.forEach { (_, ir) ->
            if (ir.fromRoad === road || ir.toRoad === road) {
                roadConnections.add(start to ir)
            }
        }
        end.intersectionRoads.forEach { (_, ir) ->
            if (ir.fromRoad === road || ir.toRoad === road) {
                roadConnections.add(end to ir)
            }
        }
    }

    override fun apply(layout: Layout) {
        layout.roads[road.id]?.let { roadToDelete ->
            layout.deleteRoad(roadToDelete)
        }
    }

    override fun revert(layout: Layout) {
        if (!layout.intersections.contains(start.id)) layout.pushIntersection(start)
        if (!layout.intersections.contains(end.id)) layout.pushIntersection(end)

        layout.pushRoad(road)

        roadConnections.forEach { (intersection, originalIr) ->
            intersection.intersectionRoads[originalIr.id] = originalIr
        }

        if (startSignal != null) {
            start.signals[road] = startSignal
        }
        if (endSignal != null) {
            end.signals[road] = endSignal
        }
    }
}
