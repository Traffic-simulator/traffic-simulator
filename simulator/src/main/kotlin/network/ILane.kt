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

    fun getPrevVehicle(vehicle: Vehicle): Pair<Vehicle?, Double>
}
