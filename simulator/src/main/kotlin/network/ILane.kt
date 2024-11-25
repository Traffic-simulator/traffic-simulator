package network

import vehicle.Direction
import vehicle.Vehicle

/**
 * Just for reading simplicity
 */
interface ILane {

    fun addVehicle(vehicle: Vehicle)

    fun removeVehicle(vehicle: Vehicle)

    fun getMinPositionVehicle(): Vehicle?

    // TODO: Check with reference line random directions
    fun getNextVehicle(vehicle: Vehicle): Pair<Vehicle?, Double>

    // TODO: Check with reference line random directions
    fun getPrevVehicle(vehicle: Vehicle): Pair<Vehicle?, Double>
}
