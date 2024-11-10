package network

import opendrive.TRoadLanes
import opendrive.TRoadLanesLaneSectionLcrLaneLink
import opendrive.TRoadLanesLaneSectionLrLane
import opendrive.TRoadLanesLaneSectionRightLane
import vehicle.Vehicle

class Lane(val tlane: TRoadLanesLaneSectionLrLane, val road: Road, val laneId: Int) {

    val vehicles: ArrayList<Vehicle> = ArrayList()

    val laneLink: TRoadLanesLaneSectionLcrLaneLink = tlane.link
    // null if junction, else list of Lane objs
    var predecessor: List<Lane>? = null
    var successor: List<Lane>? = null


    fun addVehicle(vehicle: Vehicle) {
        vehicles.add(vehicle)
    }

    fun getNextVehicle(vehicle: Vehicle): Vehicle? {
        var result: Vehicle? = null
        vehicles.forEach{ it ->
            if (it.position > vehicle.position + 0.001) {
                if (result == null) {
                    result = it
                } else {
                    if (result!!.position > it.position) {
                        result = it
                    }
                }
            }
        }

        return result
    }

    fun getNextVehicleDistance(vehicle: Vehicle): Double {
        val result: Vehicle? = getNextVehicle(vehicle)
        assert(result != null)
        return result!!.position - result.length - vehicle.position
    }

    fun getPrevVehicle(vehicle: Vehicle): Vehicle? {
        var result: Vehicle? = null
        vehicles.forEach{ it ->
            if (it.position < vehicle.position + 0.001) {
                if (result == null) {
                    result = it
                } else {
                    if (result!!.position < it.position) {
                        result = it
                    }
                }
            }
        }
        return result
    }

    fun getPrevVehicleDistance(vehicle: Vehicle): Double {
        val result: Vehicle? = getPrevVehicle(vehicle)
        assert(result != null)
        return vehicle.position - vehicle.length - result!!.position
    }

    fun getFirstVehicle(): Vehicle? {
        if (vehicles.size == 0) {
            return null
        }
        var result = vehicles[0]
        vehicles.forEach({it -> if (it.position > result.position) result = it })
        return result
    }

    fun getLastVehicle(): Vehicle? {
        if (vehicles.size == 0) {
            return null
        }
        var result = vehicles[0]
        vehicles.forEach({it -> if (it.position > result.position) result = it })
        return result
    }

    //TODO get next lane
}