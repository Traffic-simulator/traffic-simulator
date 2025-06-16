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

class Lane(val tlane: TRoadLanesLaneSectionLrLane, val road: Road, val laneId: Int) {

    val vehicles: ArrayList<Vehicle> = ArrayList()
    var laneLink: TRoadLanesLaneSectionLcrLaneLink? = tlane.link
    val roadId = road.id

    var direction: Direction? = null
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

    fun addVehicle(vehicle: Vehicle) = vehicles.add(vehicle)
    fun removeVehicle(vehicle: Vehicle) = vehicles.remove(vehicle)

    fun getMinPositionVehicle(): Vehicle? = vehicles.minByOrNull { it.position }
    fun getMaxPositionVehicle(): Vehicle? = vehicles.maxByOrNull { it.position }


    // TODO: If predecessor.size > 1 have to do graph traversal...
    fun getPrevVehicle(vehicle: Vehicle): Pair<Vehicle?, Double> {
        val prevVehicle = vehicles
            .filter { it.position < vehicle.position }
            .maxByOrNull { it.position }

        return prevVehicle?.let {
            val distance = vehicle.position - vehicle.length - it.position
            Pair(it, distance)
        } ?: Pair(null, SimulationConfig.INF)
    }

    fun getNextLane(): List<Lane>? {
        if (direction == null) {
            throw RuntimeException("Lane@$laneId direction was not properly initalized")
        }
        if (direction == Direction.FORWARD)
            return successor?.map { it.first }?.toList()
        return predecessor?.map{ it.first }?.toList()
    }
}

