package path_builder

import network.Lane
import vehicle.Vehicle

interface IPathBuilder {

    // Mandatory Lane Change - mlc
    // We can easily track PWType by sign(mlcmxRoadOffset) but let's make it clearer
    // until next PathWaypoint has MLC type -> we need to do MLC.
    // current PathWaypoint can not show info
    enum class PWType {
        MLC,
        NORMAL
    }

    data class PathWaypoint (
        val type: PWType,
        val lane: Lane,
        val mlcMaxRoadOffset: Double
    )

    fun isDestinationReachable(vehicle: Vehicle, initPosition: Double): Boolean

    fun getNextMLCDistance(vehicle: Vehicle): Double?

    fun getNextPathLane(vehicle: Vehicle, lane: Lane): PathWaypoint?

    fun getNextPathLane(vehicle: Vehicle): PathWaypoint?

    // Finds vehicles on path before closest Mandatory Lane Change
    fun getNextVehicle(vehicle: Vehicle): Pair<Vehicle?, Double>

    fun removePath(vehicle: Vehicle)
}
