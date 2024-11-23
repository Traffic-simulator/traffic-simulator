package network

import SimulationConfig
import opendrive.TRoadLanes
import opendrive.TRoadLanesLaneSectionLcrLaneLink
import opendrive.TRoadLanesLaneSectionLrLane
import opendrive.TRoadLanesLaneSectionRightLane
import vehicle.Vehicle


// Assuming that vehicle moves to the next_lane after front bumper (position) is > length(previous_lane)
// TODO: Test functions with very short lane segments
// TODO: Completely not working with junctions
class Lane(val tlane: TRoadLanesLaneSectionLrLane, val road: Road, val laneId: Int) {

    // TODO: need clever structure for binary_search and easy .front() .back()
    private val vehicles: ArrayList<Vehicle> = ArrayList()
    val laneLink: TRoadLanesLaneSectionLcrLaneLink? = tlane.link
    val roadId = road.id

    // TODO: flag if the next lane is from junction
    // List for lanes from roads AND from junctions and for strange asam.net:xodr:1.4.0:road.lane.link.multiple_connections.
    var predecessor: ArrayList<Lane>? = null
    var successor: ArrayList<Lane>? = null

    // TODO: Handle move to the next line
    fun addVehicle(vehicle: Vehicle) {
        vehicles.add(vehicle)
    }

    fun removeVehicle(vehicle: Vehicle) {
        vehicles.remove(vehicle)
    }

    fun getNextVehicle(vehicle: Vehicle): Pair<Vehicle?, Double> {
        var result: Vehicle? = null

        // TODO: use binary search
        vehicles.forEach{ it ->
            if (it.position > vehicle.position + SimulationConfig.EPS) {
                if (result == null) {
                    result = it
                } else {
                    if (result!!.position > it.position) {
                        result = it
                    }
                }
            }
        }

        if (result != null) {
            val distance = result!!.position - result!!.length - vehicle.position
            if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }
            return Pair(result, distance)
        }

        val nextLane = if (laneId > 0) successor else predecessor
        if (nextLane == null) {
            return Pair(null, SimulationConfig.INF)
        }

        // TODO: If successor.size > 1 do we need graph traversal? Or only the our next line. Because junction will block if need
        return nextLane.get(0).getLastVehicle(road.troad.length - vehicle.position)
    }

    private fun getLastVehicle(accDistance: Double): Pair<Vehicle?, Double> {

        val nextLane = if (laneId > 0) successor else predecessor
        if (vehicles.size == 0) {
            if (nextLane == null || accDistance + road.troad.length > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }

            // TODO: If successor.size > 1 do we need graph traversal? Or only the our next line. Because junction will block if need
            return nextLane.get(0).getLastVehicle(accDistance + road.troad.length)
        }

        var result = vehicles[0]
        vehicles.forEach({it -> if (it.position < result.position) result = it })
        val distance = accDistance + result.position - result.length
        if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
            return Pair(null, SimulationConfig.INF)
        }
        return Pair(result, distance)
    }


    // TODO: Check with reference line random directions
    fun getPrevVehicle(vehicle: Vehicle): Pair<Vehicle?, Double> {
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


        val nextLane = if (laneId < 0) successor else predecessor
        if (nextLane == null) {
            return Pair(null, SimulationConfig.INF)
        }

        // TODO: If predecessor.size > 1 have to do graph traversal...
        return nextLane.get(0).getFirstVehicle(vehicle.position - vehicle.length)
    }


    // TODO: Check with reference line random directions
    private fun getFirstVehicle(accDistance: Double): Pair<Vehicle?, Double> {
        val nextLane = if (laneId < 0) successor else predecessor
        if (vehicles.size == 0) {
            if (nextLane == null || accDistance + road.troad.length > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                return Pair(null, SimulationConfig.INF)
            }

            // TODO: If predecessor.size > 1 have to do graph traversal...
            return nextLane.get(0).getFirstVehicle(accDistance + road.troad.length)
        }

        var result = vehicles[0]
        vehicles.forEach({it -> if (it.position > result.position) result = it })

        val distance = accDistance + road.troad.length - result.position
        if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
            return Pair(null, SimulationConfig.INF)
        }
        return Pair(result, distance)
    }

}

