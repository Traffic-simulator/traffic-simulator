package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec2
import kotlin.math.abs
import kotlin.math.sign

class Intersection(
    val id: Long, var position: Vec2, padding: Double = 0.0, var building: Building? = null
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: MutableMap<Long, IntersectionRoad> = HashMap()
    var signals: HashMap<Road, Signal> = HashMap()
    private var irNextId: Long = 0

    val isBuilding: Boolean get() = building != null
    val hasSignals: Boolean get() = signals.isNotEmpty()
    val incomingRoadsCount get() = incomingRoads.size
    var padding = padding
        set(value) {
            if (incomingRoads.any { !it.ableToSetPadding(field, value) }) {
                throw IllegalArgumentException("Can't set padding to $value")
            }
            field = value
            recalculateIntersectionRoads()
        }

    override fun toString(): String {
        return "Intersection(id=$id, position=$position, building=$building, signals=$signals)"
    }

    fun connectRoad(road: Road) {
        removeRoad(road)
        for (incomingRoad in incomingRoads) {
            if (incomingRoad !== road) {
                addIntersectionRoad(road, incomingRoad)
                addIntersectionRoad(incomingRoad, road)
            }
        }
        addRoad(road)
    }

    private fun addIntersectionRoad(fromRoad: Road, toRoad: Road) {
        val incomingLaneNumber = fromRoad.getIncomingLaneNumber(this)
        val outgoingLaneNumber = toRoad.getOutgoingLaneNumber(this)

        val incomingSign = incomingLaneNumber.sign
        val outgoingSign = outgoingLaneNumber.sign

        for (incomingLane in 1..abs(incomingLaneNumber)) {
            for (outgoingLane in 1..abs(outgoingLaneNumber)) {
                val geometry = Spline()

                val newIntersectionRoad = IntersectionRoad(
                    id = irNextId++,
                    intersection = this,
                    fromRoad = fromRoad,
                    toRoad = toRoad,
                    geometry = geometry,
                    laneLinkage = incomingLane * incomingSign to outgoingLane * outgoingSign
                )
                newIntersectionRoad.recalculateGeometry()

                intersectionRoads[newIntersectionRoad.id] = newIntersectionRoad
            }
        }
    }

    fun addRoad(road: Road) {
        if (isBuilding && incomingRoads.size > 0) {
            throw IllegalArgumentException("Building cannot have more than one road")
        }
        incomingRoads.add(road)
    }

    fun removeRoad(road: Road) {
        if (isBuilding) {
            throw IllegalArgumentException("Cannot remove road from building")
        }

        incomingRoads.remove(road)
        intersectionRoads.values.toList().forEach {
            if (it.toRoad === road || it.fromRoad === road) intersectionRoads.remove(it.id)
        }
        signals.remove(road)
    }

    fun recalculateIntersectionRoads() = intersectionRoads.forEach { (_, intersectionRoad) ->
        intersectionRoad.recalculateGeometry()
    }

    fun recalculateIntersectionRoads(road: Road) = intersectionRoads.forEach { (_, intersectionRoad) ->
        if (intersectionRoad.fromRoad === road || intersectionRoad.toRoad === road) {
            intersectionRoad.recalculateGeometry()
        }
    }
}

