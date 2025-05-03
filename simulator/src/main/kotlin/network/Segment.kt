package network

import opendrive.TRoad
import vehicle.Vehicle

class Segment {
    val lastStates = ArrayDeque<Double>()
    val currentState: Double = 0.0
    val currentIterVehicles: MutableList<Vehicle> = mutableListOf()

    fun update(vehiclesOnSegment: List<Vehicle>) {
        // чистим машинки на этой итерации
        // считаем взвешенное новое значение, закидываем его в lastStates
    }

    fun addVehicle(vehicle: Vehicle) {
        currentIterVehicles.add(vehicle)
    }
}
