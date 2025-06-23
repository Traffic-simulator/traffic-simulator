package ru.nsu.trafficsimulator.backend.route

import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.IRegionPathAPI
import ru.nsu.trafficsimulator.backend.path.RegionPathAPI

// TODO: connect with info from stats.xodr
// It's proxy above simple RegionRouteGenerator
// But it has to know much more info than common RouteGenerator
// Goal of this class work with underlying SpawnModel to spawn vehicles only on regionRoads
class RegionRouteGenerator(
    private val regionId: Int,
    private val network: Network,
    private val base: IRouteGenerator
) : IRouteGenerator {

    private val regionAPI: IRegionPathAPI = RegionPathAPI(network)
    var counter: Int = 0

    data class SimulationVehicleInfo(
        var isInSimulation: Boolean,
        var localSource: Waypoint, // have to be in the regionId
        var localDest: Waypoint,   // have to be in the regionId
        var afterLocalDest: Waypoint?,  // have NOT to be in the regionId
        var simVehId: Int
    )

    data class VehicleInfo(
        // ConstantInfo
        val vehicleId: Int,
        val despawnCallback: RouteGeneratorDespawnListener,
        val globalSource: Waypoint,
        val globalDest: Waypoint,

        // ChangableInfo
        var timeout: Double, // In seconds
        var simInfo: SimulationVehicleInfo?,
        var toRemove: Boolean = false
    )

    private val vehicles = ArrayList<VehicleInfo>()

    // base on despawn can be called only in vehicles.forEach
    override fun update(dt: Double, simCreator: VehicleCreationListener, simPositionChecker: WaypointSpawnAbilityChecker) {

        // Эта функция не обязательно спавнит машинку, возможно она просто готовит структуры для последующего вызова
        // source, destination могут быть как в нашем регионе, так и не в нашем.
        fun updateVehicleState(source: Waypoint, destination: Waypoint, baseModelOnDespawn: RouteGeneratorDespawnListener, regionOnDespawn: RouteGeneratorDespawnListener, regionModelVehId_: Int?): Int {
            // TODO: cost have to be in seconds
            //  ---------------------------------------------------------------
                val (cost, path) = regionAPI.getGlobalPath(source, destination)
                val regionModelVehId = regionModelVehId_ ?: counter++
                if (regionModelVehId_ == null) {
                    vehicles.add(VehicleInfo(regionModelVehId, baseModelOnDespawn, source, destination, 0.0, null))
                }
                val vehicleInfo = vehicles.find { it.vehicleId == regionModelVehId }!!

                // find first region road
                val firstPathWaypoint = path.find { regionAPI.isRegionRoad(it.lane.roadId, regionId) }
                if (firstPathWaypoint == null) {
                    // path is completely out of region
                    // When timeout will expire, despawner will delete it
                    vehicleInfo.simInfo = null
                    vehicleInfo.timeout = cost
                    return regionModelVehId
                }

                val lastPathWaypoint = path.subList(path.indexOf(firstPathWaypoint), path.size)
                    .takeWhile { regionAPI.isRegionRoad(it.lane.roadId, regionId) }
                    .last()
                val afterLastIdx = path.indexOf(lastPathWaypoint) + 1
                val afterLastPathWaypoint = if (afterLastIdx < path.size) path.get(afterLastIdx) else null

                val firstWaypoint = Waypoint.fromPathWaypoint(firstPathWaypoint)
                val lastWaypoint = Waypoint.fromPathWaypoint(lastPathWaypoint)
                val afterLastWaypoint = Waypoint.fromPathWaypointNullable(afterLastPathWaypoint)
            //  ------------------------------------------------------------------

            // Vehicle will try to be spawned until timeout < 0 and isInSimulation = false
            if (firstPathWaypoint == path[0] && firstWaypoint.equals(vehicleInfo.globalSource)) {
                // Option 1) Vehicle is spawning from house in our region (possible only from direct base model call)
                if (simPositionChecker.isFree(firstWaypoint)) {
                    val simVehId = simCreator.createVehicle(firstWaypoint, lastWaypoint, regionOnDespawn)
                    val simInfo  = SimulationVehicleInfo(true, firstWaypoint, lastWaypoint, afterLastWaypoint, simVehId)
                    // In this case timeout can be any you want
                    vehicleInfo.simInfo = simInfo
                }
            } else if (firstPathWaypoint == path[0]) {
                // Option 2) Vehicle is spawning on speed in our region
                val spawnSpeed = regionAPI.getWaypointAvgSpeed(firstWaypoint)
                if (simPositionChecker.isFreeWithSpeed(firstWaypoint, lastWaypoint, spawnSpeed)) {
                    val simVehId = simCreator.createVehicle(firstWaypoint, lastWaypoint, regionOnDespawn, spawnSpeed)
                    val simInfo  =  SimulationVehicleInfo(true, firstWaypoint, lastWaypoint, afterLastWaypoint, simVehId)
                    // In this case timeout can be any you want
                    vehicleInfo.simInfo = simInfo
                }
            } else {
                // Option 3) Vehicle will later run into our region
                // vehicle will be spawned when timeout expire
                vehicleInfo.simInfo = SimulationVehicleInfo(false, firstWaypoint, lastWaypoint, afterLastWaypoint, -1)
                vehicleInfo.timeout = regionAPI.getPathTime(path, firstPathWaypoint)
            }

            return regionModelVehId
        }

        // defining despawnLogic
        val regionOnDespawn = object : RouteGeneratorDespawnListener {
            override fun onDespawn(vehicleId: Int) {
                val veh = vehicles.find { it.simInfo!= null && it.simInfo!!.simVehId == vehicleId}
                // update vehicleInfo
                veh!!.simInfo!!.isInSimulation = false
                if (veh.simInfo!!.afterLocalDest == null) {
                    // This vehicle will be despawned
                    veh.simInfo = null
                    return
                }
                updateVehicleState(veh.simInfo!!.afterLocalDest!!, veh.globalDest, veh.despawnCallback, this, veh.vehicleId)
            }
        }

        // Handle existing vehicles
        vehicles.forEach {
            it.timeout -= dt
            if (it.timeout < 0.0) {
                if (it.simInfo == null){
                    // If simInfo is set to null - that vehicle have to be removed
                    it.despawnCallback.onDespawn(it.vehicleId)
                    it.toRemove = true
                } else if (!it.simInfo!!.isInSimulation){
                    // We can now try to update this vehicle
                    updateVehicleState(it.simInfo!!.localSource, it.simInfo!!.localDest, it.despawnCallback, regionOnDespawn, it.vehicleId)
                }
            }
        }
        // TODO: Check that it's actually removing
        vehicles.removeAll { it.toRemove == true}

        val regionCreateVehicle = object : VehicleCreationListener {
            override fun createVehicle(
                source: Waypoint,
                destination: Waypoint,
                baseModelOnDespawn: RouteGeneratorDespawnListener,
                initialSpeed: Double
            ): Int {
                return updateVehicleState(source, destination, baseModelOnDespawn, regionOnDespawn, null)
            }

            override fun getWaypointByJunction(junctionId: String, isStart: Boolean): Waypoint {
                return simCreator.getWaypointByJunction(junctionId, isStart)
            }
        }

        val regionPositionChecker = object : WaypointSpawnAbilityChecker {
            // if the road is in region call simple isFree
            // if the road is not in the region answer is true // TODO: add some delay between this true
            override fun isFree(waypoint: Waypoint): Boolean {
                if (regionAPI.isRegionRoad(waypoint.roadId.toInt(),regionId)) {
                    return simPositionChecker.isFree(waypoint)
                } else {
                    return true
                }
            }

            override fun isFreeWithSpeed(source: Waypoint, destination: Waypoint, speed: Double): Boolean {
                TODO("Inner model does not have to use speed spawning")
            }
        }

        // Update inner model
        base.update(dt, regionCreateVehicle, regionPositionChecker)
    }

}
