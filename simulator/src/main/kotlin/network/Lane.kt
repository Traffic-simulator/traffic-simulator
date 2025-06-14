package network

import SimulationConfig
import heatmap.Segment
import network.signals.Signal
import opendrive.TRoadLanesLaneSectionLcrLaneLink
import opendrive.TRoadLanesLaneSectionLrLane
import vehicle.Direction
import vehicle.Vehicle
import kotlin.math.max
import kotlin.math.roundToInt

class Lane(val tlane: TRoadLanesLaneSectionLrLane, val road: Road, val laneId: Int): ILane {

    val vehicles: ArrayList<Vehicle> = ArrayList()
    var laneLink: TRoadLanesLaneSectionLcrLaneLink? = tlane.link
    val roadId = road.id

    // List for lanes from roads AND from junctions and for strange asam.net:xodr:1.4.0:road.lane.link.multiple_connections.
    var predecessor: ArrayList<Pair<Lane, Boolean>>? = null
    var successor: ArrayList<Pair<Lane, Boolean>>? = null

    var signal: Signal? = null

    val length: Double = road.troad.length
    val lenOfSegment: Double = 10.0
    var segments: List<Segment> = List(max(length.div(lenOfSegment).roundToInt(), 1)) {
        index ->
        Segment(this)
    }

    fun getMaxSpeed(): Double {
        val defaultMaxSpeed = 30.0
        val res = road.troad.type
        if (res != null && !res.isEmpty()) {
            return res.get(0)?.speed?.max?.toDouble() ?: defaultMaxSpeed
        }
        return defaultMaxSpeed
    }

    override fun addVehicle(vehicle: Vehicle) {
        vehicles.add(vehicle)
    }

    override fun removeVehicle(vehicle: Vehicle) {
        vehicles.remove(vehicle)
    }

    override fun getMinPositionVehicle(): Vehicle? {
        var result: Vehicle? = null

        vehicles.forEach{
            if (result == null) {
                result = it
            }
            if (result != null && it.position < result!!.position) {
                result = it
            }
        }

        return result
    }

    fun getMaxPositionVehicle(): Vehicle? {
        var result: Vehicle? = null

        vehicles.forEach{
            if (result == null) {
                result = it
            }
            if (result != null && it.position > result!!.position) {
                result = it
            }
        }

        return result
    }

    override fun getPrevVehicle(vehicle: Vehicle): Pair<Vehicle?, Double> {
        var result: Vehicle? = null

        vehicles.forEach{ it ->
            if (it.position < vehicle.position) {
                if (result == null) {
                    result = it
                } else {
                    if (result!!.position < it.position) {
                        result = it
                    }
                }
            }
        }

        if (result != null) {
            val distance = vehicle.position - vehicle.length - result!!.position
            if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }
            return Pair(result, distance)
        }

        return Pair(null, SimulationConfig.INF)
        // TODO: If predecessor.size > 1 have to do graph traversal...
    }

    fun getNextLane(direction: Direction): ArrayList<Pair<Lane, Boolean>>? {
        if (direction == Direction.FORWARD)
            return successor
        return predecessor
    }
}

