package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Vec2

data class Intersection(
    val id: Long,
    var position: Vec2,
    var padding: Double = 0.0,
    var building: Building? = null
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: HashSet<IntersectionRoad> = HashSet()
    var signals: HashMap<Road, Signal> = HashMap()

    val isBuilding: Boolean get() = building != null
    val hasSignals: Boolean get() = signals.isNotEmpty()
    val incomingRoadsCount get() = incomingRoads.size

    override fun toString(): String {
        return "Intersection(id=$id, position=$position, building=$building, signals=$signals)"
    }

    fun addRoad(road: Road) {
        if (isBuilding && incomingRoads.size > 0) {
            throw IllegalArgumentException("Building cannot have more than one road")
        }
        incomingRoads.add(road)
        if (hasSignals) {
            signals[road] = Signal()
        }
    }

    fun removeRoad(road: Road) {
        if (isBuilding) {
            throw IllegalArgumentException("Cannot remove road from building")
        }

        incomingRoads.remove(road)
        intersectionRoads.removeIf { it.toRoad === road || it.fromRoad === road }
        signals.remove(road)
    }

    fun recalculateIntersectionRoads() {
        for (intersectionRoad in intersectionRoads) {
            intersectionRoad.recalculateGeometry()
        }
    }

    fun recalculateIntersectionRoads(road : Road) {
        for (intersectionRoad in intersectionRoads) {
            if (intersectionRoad.fromRoad === road || intersectionRoad.toRoad === road) {
                intersectionRoad.recalculateGeometry()
            }
        }
    }
}
