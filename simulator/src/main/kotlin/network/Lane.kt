package network

import SimulationConfig
import network.signals.Signal
import opendrive.TRoadLanesLaneSectionLcrLaneLink
import opendrive.TRoadLanesLaneSectionLrLane
import vehicle.Direction
import vehicle.Vehicle


// Assuming that vehicle moves to the next_lane after front bumper (position) is > length(previous_lane)
// TODO: Test functions with very short lane segments
// TODO: Completely not working with junctions
class Lane(val tlane: TRoadLanesLaneSectionLrLane, val road: Road, val laneId: Int): ILane {

    // TODO: need clever structure for binary_search and easy .front() .back()
    val vehicles: ArrayList<Vehicle> = ArrayList()
    var laneLink: TRoadLanesLaneSectionLcrLaneLink? = tlane.link
    val roadId = road.id

    // TODO: flag if the next lane is from junction
    // List for lanes from roads AND from junctions and for strange asam.net:xodr:1.4.0:road.lane.link.multiple_connections.
    var predecessor: ArrayList<Pair<Lane, Boolean>>? = null
    var successor: ArrayList<Pair<Lane, Boolean>>? = null

    var signal: Signal? = null

    override fun addVehicle(vehicle: Vehicle) {
        vehicles.add(vehicle)
    }

    override fun removeVehicle(vehicle: Vehicle) {
        vehicles.remove(vehicle)
    }

    override fun getMinPositionVehicle(): Vehicle? {
        var result: Vehicle? = null

        // TODO: use binary search
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

    private fun getLastVehicleInternal(direction: Direction, accDistance: Double): Pair<Vehicle?, Double> {

        val nextLane = getNextLane(direction)
        if (vehicles.size == 0) {
            if (nextLane == null || nextLane.isEmpty() || accDistance + road.troad.length > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }

            // TODO: If successor.size > 1 do we need graph traversal? Or only the our next line. Because junction will block if need
            return nextLane.get(0).first.getLastVehicleInternal(direction.opposite(nextLane.get(0).second), accDistance + road.troad.length)
        }

        var result = vehicles[0]
        vehicles.forEach({it -> if (it.position < result.position) result = it })
        val distance = accDistance + result.position - result.length
        if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
            return Pair(null, SimulationConfig.INF)
        }
        return Pair(result, distance)
    }

    fun mockNextVehicle(): Pair<Vehicle?, Double> {
        return Pair(null, SimulationConfig.INF)
    }

    // TODO: Check with reference line random directions
    override fun getPrevVehicle(vehicle: Vehicle): Pair<Vehicle?, Double> {
        var result: Vehicle? = null

        // TODO: use binary search
        vehicles.forEach{ it ->
            if (it.position < vehicle.position + SimulationConfig.EPS) {
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
        // TODO: Check until junction probably... and
    }


    // TODO: Check with reference line random directions
    private fun getFirstVehicleInternal(direction: Direction, accDistance: Double): Pair<Vehicle?, Double> {
        val nextLane = getNextLane(direction)
        if (vehicles.size == 0) {
            if (nextLane == null || nextLane.isEmpty() || accDistance + road.troad.length > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }

            // TODO: If predecessor.size > 1 have to do graph traversal...
            return nextLane.get(0).first.getFirstVehicleInternal(direction.opposite(nextLane.get(0).second), accDistance + road.troad.length)
        }

        var result = vehicles[0]
        vehicles.forEach({it -> if (it.position > result.position) result = it })

        val distance = accDistance + road.troad.length - result.position
        if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
            return Pair(null, SimulationConfig.INF)
        }
        return Pair(result, distance)
    }

    fun getNextLane(direction: Direction): ArrayList<Pair<Lane, Boolean>>? {
        if (direction == Direction.FORWARD)
            return successor
        return predecessor
    }

}

