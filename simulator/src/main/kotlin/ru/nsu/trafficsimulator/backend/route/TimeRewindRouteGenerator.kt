package ru.nsu.trafficsimulator.backend.route

import ru.nsu.trafficsimulator.backend.Simulator
import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.algorithms.DijkstraPathBuilder
import ru.nsu.trafficsimulator.backend.path.algorithms.IPathBuilder
import ru.nsu.trafficsimulator.backend.path.cost_function.StatsCostFunction

class TimeRewindRouteGenerator(
    private val network: Network,
    private val startingTime: Double,
    private val base: IRouteGenerator
) : IRouteGenerator {

    private val vehiclesDespawnList = HashMap<Int, Pair<Double, RouteGeneratorDespawnListener>>()
    private val dijkstraPathBuilder: IPathBuilder = DijkstraPathBuilder(network, StatsCostFunction())
    private var counter = -1

    override fun update(dt: Double, create: VehicleCreationListener, isPositionFree: WaypointSpawnAbilityChecker) {
        handleDespawnList(dt)
        base.update(dt, create, isPositionFree)
    }

    fun handleDespawnList(dt: Double) {
        vehiclesDespawnList.forEach { (key, value) ->
            vehiclesDespawnList[key] = value.copy(first = value.first - dt)
        }

        vehiclesDespawnList.entries.removeAll { (id, vehicle) ->
            if (vehicle.first < 0) {
                vehicle.second.onDespawn(id)
                true
            } else {
                false
            }
        }
    }

    private val timeRewinderPositionChecker = object : WaypointSpawnAbilityChecker {
        override fun isFree(waypoint: Waypoint): Boolean {
            return true
        }

        override fun isFreeWithSpeed(source: Waypoint, destination: Waypoint, speed: Double): Boolean {
            return true
        }
    }

    private val createVehicle = object : VehicleCreationListener {
        override fun createVehicle(
            source: Waypoint,
            destination: Waypoint,
            onDespawn: RouteGeneratorDespawnListener,
            initialSpeed: Double
        ): Int {
            // Will assign negative ids
            val vehId = counter--
            val time = dijkstraPathBuilder.getPath(source, destination, 0.0).first
            vehiclesDespawnList.put(vehId, Pair(time, onDespawn))
            return vehId
        }

        override fun getWaypointByJunction(junctionId: String, isStart: Boolean): Waypoint {
            return network.getWaypointByJunction(junctionId, isStart)
        }
    }

    init {
        var currentTime = 0.0
        val dt = 0.5

        while (currentTime < startingTime) {
            handleDespawnList(dt)
            base.update(dt, createVehicle, timeRewinderPositionChecker)
            currentTime += dt
        }
    }
}
