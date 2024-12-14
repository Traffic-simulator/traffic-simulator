package path_builder

import network.Lane
import vehicle.Direction
import vehicle.Vehicle

interface IPathBuilder {

    fun getNextPathLane(vehicle: Vehicle): Pair<Lane, Boolean>?

    fun getNextPathLane(vehicle: Vehicle, lane: Lane, direction: Direction): Pair<Lane, Boolean>?

    fun removePath(vehicle: Vehicle)
}
