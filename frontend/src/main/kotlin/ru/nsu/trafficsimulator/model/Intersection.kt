package ru.nsu.trafficsimulator.model

import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.intsettings.BuildingIntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.IntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.MergingIntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.SplitDistrictsIntersectionSettings
import kotlin.math.abs
import kotlin.math.sign

class Intersection(
    var id: Long,
    var position: Vec2,
    padding: Double = 0.0,
    var intersectionSettings: IntersectionSettings? = null
) {
    val incomingRoads: MutableSet<Road> = HashSet()
    val intersectionRoads: MutableMap<Long, IntersectionRoad> = HashMap()
    var signals: HashMap<Road, Signal> = HashMap()
    private var irNextId: Long = 0

    val building: BuildingIntersectionSettings?
        get() = if (intersectionSettings != null && intersectionSettings is BuildingIntersectionSettings) {
            intersectionSettings as BuildingIntersectionSettings
        } else null
    val isBuilding: Boolean
        get() = building != null

    val merging: MergingIntersectionSettings?
        get() = if (intersectionSettings != null && intersectionSettings is MergingIntersectionSettings) {
            intersectionSettings as MergingIntersectionSettings
        } else null
    val isMerging: Boolean
        get() = merging != null

    val splitSettings: SplitDistrictsIntersectionSettings?
        get() = if (intersectionSettings != null && intersectionSettings is SplitDistrictsIntersectionSettings) {
            intersectionSettings as SplitDistrictsIntersectionSettings
        } else null
    val isSplitting: Boolean
        get() = splitSettings != null

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
        if (incomingRoads.isNotEmpty()) {
            if (isMerging) {
                throw IllegalArgumentException("Merging intersection can has only one road!")
            }
            if (isBuilding) {
                throw IllegalArgumentException("Building intersection can has only one road!")
            }
        }

        intersectionRoads.values.toList().forEach {
            if (it.toRoad === road || it.fromRoad === road) intersectionRoads.remove(it.id)
        }

        for (incomingRoad in incomingRoads) {
            if (incomingRoad !== road) {
                addIntersectionRoad(road, incomingRoad)
                addIntersectionRoad(incomingRoad, road)
            }
        }

        addRoad(road)
    }

    fun disconnectLanes(fromRoad: Road, fromLane: Int, toRoad: Road, toLane: Int): IntersectionRoad? {
        if (incomingRoads.contains(fromRoad) && incomingRoads.contains(toRoad)) {
            return findConnectingRoad(fromRoad, fromLane, toRoad, toLane)?.also {
                intersectionRoads.remove(it.id)
            }
        }
        return null
    }

    fun connectLanes(fromRoad: Road, fromLane: Int, toRoad: Road, toLane: Int): IntersectionRoad? {
        if (incomingRoads.contains(fromRoad) && incomingRoads.contains(toRoad)) {
            findConnectingRoad(fromRoad, fromLane, toRoad, toLane) ?: run {
                val geometry = Spline()

                val newIntersectionRoad = IntersectionRoad(
                    id = irNextId++,
                    intersection = this,
                    fromRoad = fromRoad,
                    toRoad = toRoad,
                    geometry = geometry,
                    laneLinkage = fromLane to toLane,
                    district = fromRoad.district // maybe tak, sporno
                )
                newIntersectionRoad.recalculateGeometry()

                intersectionRoads[newIntersectionRoad.id] = newIntersectionRoad

                return newIntersectionRoad
            }
        }
        return null
    }

    fun findConnectingRoad(fromRoad: Road, fromLane: Int, toRoad: Road, toLane: Int): IntersectionRoad? =
        intersectionRoads.values.find {
            it.fromRoad === fromRoad && it.toRoad === toRoad
                && it.laneLinkage == fromLane to toLane
        }


    private fun addIntersectionRoad(fromRoad: Road, toRoad: Road) {
        val incomingLaneNumber = fromRoad.getIncomingLaneCount(this)
        val outgoingLaneNumber = toRoad.getOutgoingLaneCount(this)

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
                    laneLinkage = incomingLane * incomingSign to outgoingLane * outgoingSign,
                    district = fromRoad.district
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
        if (hasSignals) {
            signals[road] = Signal()
        }
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

    @Deprecated("Only for undo/redo")
    fun pushIntersectionRoad(intersectionRoad: IntersectionRoad) {
        if (intersectionRoad.intersection === this
            && !intersectionRoads.containsKey(intersectionRoad.id)
            && findConnectingRoad(
                intersectionRoad.fromRoad,
                intersectionRoad.laneLinkage.first,
                intersectionRoad.toRoad,
                intersectionRoad.laneLinkage.second
            ) == null
        ) {
            intersectionRoads[intersectionRoad.id] = intersectionRoad
        } else {
            logger.debug("Cannot push intersection road")
        }
    }

    @Deprecated("Only for undo/redo")
    fun deleteIntersectionRoad(intersectionRoad: IntersectionRoad) {
        if (intersectionRoads.containsKey(intersectionRoad.id)) {
            intersectionRoads.remove(intersectionRoad.id)
        } else {
            logger.debug("Cannot delete intersection road")
        }
    }
}

