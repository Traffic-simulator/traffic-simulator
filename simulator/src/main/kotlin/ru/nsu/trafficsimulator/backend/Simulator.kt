package ru.nsu.trafficsimulator.backend

import ISimulation
import ru.nsu.trafficsimulator.backend.junction_intersection.JunctionIntersectionFinder
import mu.KotlinLogging
import ru.nsu.trafficsimulator.backend.network.Network
import opendrive.ERoadLinkElementType
import ru.nsu.trafficsimulator.backend.network.Road
import ru.nsu.trafficsimulator.backend.network.Waypoint
import opendrive.OpenDRIVE
import ru.nsu.trafficsimulator.backend.route_generator.IRouteGenerator
import ru.nsu.trafficsimulator.backend.route_generator.RouteGeneratorDespawnListener
import ru.nsu.trafficsimulator.backend.route_generator.VehicleCreationListener
import ru.nsu.trafficsimulator.backend.route_generator.WaypointSpawnAbilityChecker
import ru.nsu.trafficsimulator.backend.route_generator_new.RouteGeneratorImpl
import ru.nsu.trafficsimulator.backend.route_generator_new.discrete_function.Building
import vehicle.Direction
import ru.nsu.trafficsimulator.backend.vehicle.Vehicle
import ru.nsu.trafficsimulator.backend.vehicle.Vehicle.Companion.pathManager
import ru.nsu.trafficsimulator.backend.vehicle.VehicleDetector
import ru.nsu.trafficsimulator.backend.vehicle.model.IDM
import java.time.LocalTime
import kotlin.collections.ArrayList
import kotlin.random.Random

// Route - source point and destination point.
// Path - all concrete roads and lanes that vehicle will go
class Simulator(openDrive: OpenDRIVE,
                drivingSide: ISimulation.DrivingSide,
                startingTime: LocalTime,
                seed: Long,
                numFramesHeatmapMemory: Int = 2000
) {

    val finder = JunctionIntersectionFinder(openDrive)
    private val logger = KotlinLogging.logger("SIMULATOR")
    val intersections = finder.findIntersection()
    val network: Network = Network(drivingSide, openDrive.road, openDrive.junction, intersections, numFramesHeatmapMemory)
    val rnd = Random(seed)
    val routeGeneratorAPI: IRouteGenerator

    val vehicles: ArrayList<Vehicle> = ArrayList()
    val buildings: List<Building>
    var currentTime: Double = startingTime.toSecondOfDay().toDouble()
    init {
        val buildingParser = BuildingsParser(openDrive)
        buildings = buildingParser.getBuildings()
        routeGeneratorAPI = RouteGeneratorImpl(currentTime, buildings, seed)
    }

    fun update(dt: Double): ArrayList<Vehicle> {
        /*
            Traffic Lights logic:
                1) First of all we need to unlock all trajectories that was blocked by vehicles for which RED is appeared
                2) Secondly we have to say to each vehicle on lines with RED to don't pretend to any trajectories.
                    To do that we can just check lane signal for each vehicle
                        and in case of RED, RED_YELLOW or YELLOW don't block any trajectories and stop
         */
        network.updateSignals(dt)
        vehicles.forEach { it.processTrafficLight() }

        /*
            Lane changes logic:
                1) Mandatory lane changes (to be able to go to destination)
                2) Non-mandatory lane changes (to speed up vehicle flow)
         */
        vehicles.forEach { it.processMLC() }
        vehicles.forEach { it.processNMLC() }

        /*
            Vehicles movement logic
                1) Process vehicles in sorted order due to junction blocking logic
                    Have to sort not by position, but by distance to the closest junction...
                2) Calculate acceleration to each vehicle with deltaTime
                3) Update vehicles acceleration
                4) Despawn arrived vehicles
         */
        val sortedVehicles = vehicles.sortedBy { it.distToClosestJunction() }
        sortedVehicles.forEach { it ->
            it.updateAcceleration()
        }
        sortedVehicles.forEach { it ->
            it.update(dt)
        }
        vehicles.removeAll { it.despawned == true}

        // delete paths of all vehicles for dynamic path calculation:
        // vehicles.forEach { veh -> veh.pathBuilder.removePath(veh) }

        /*
            Vehicle generation logic.
                1) Spawn new vehicles
        */
        routeGeneratorAPI.update(dt, createVehicle, isPositionFree)

        /*
            Statistics gathering:
                1) Update segments for heatmap on this cycle
                2) Update road-side average speed
        */
        gatherStatistics()

        currentTime += dt
        return vehicles
    }

    fun removeVehicle(vehicle: Vehicle) {
        vehicle.lane.removeVehicle(vehicle)
        vehicles.remove(vehicle)
    }

    // We can do it not each frame (but be careful with gatherSimStats in BackendAPI)
    fun gatherStatistics() {
        val roads: List<Road> = network.roads
        for (road in roads) {

            // Compute segments
            for (lane in road.lanes) {
                lane.vehicles.forEach {
                    val posFromStart = if (it.lane.direction == Direction.BACKWARD) {
                        lane.length - it.position
                    } else {
                        it.position
                    }
                    val segmentIndex = (posFromStart / lane.lenOfSegment).toInt()
                    if (segmentIndex < lane.segments.size) {
                        lane.segments[segmentIndex].addVehicleSpeed(it)
                    } else {
                        lane.segments.last().addVehicleSpeed(it)
                    }
                }
                lane.segments.forEach { it.update() }
            }

            // Roads avg speed
            fun getRoadSideAvgSpeed(roadSide: Int): Double {
                var sumSegments = 0.0
                var cntSegments = 0

                road.lanes.filter { it.laneId * roadSide > 0}.forEach {
                        itLane->
                    itLane.segments.forEach {
                            seg ->
                        sumSegments += seg.getAverageSpeed()
                        cntSegments ++
                    }
                }

                return sumSegments / cntSegments
            }

            road.positiveSideAvgSpeed = getRoadSideAvgSpeed(1)
            road.negativeSideAvgSpeed = getRoadSideAvgSpeed(-1)
        }
    }

    fun clearHeatmapData() {
        for (road in network.roads) {
            for (lane in road.lanes) {
                lane.segments.forEach { it.clearMemory() }
            }
        }
    }

    private val isPositionFree = object : WaypointSpawnAbilityChecker {
        override fun isFree(waypoint: Waypoint): Boolean {
            val lane = network.getLaneById(waypoint.roadId, waypoint.laneId.toInt())

            val minVeh = lane.getMinPositionVehicle()
            if (minVeh == null || minVeh!!.position - minVeh.length > SimulationConfig.MIN_GAP) {
                return true
            }
            return false
        }

        override fun isFreeWithSpeed(source: Waypoint, destination: Waypoint, speed: Double): Boolean {
            // Let's just create temporary vehicle to check ability of it's existence
            // maxAcc can be random here as there is no problem in cars behind us.

            val vehicle = Vehicle.createTempVehicle(network, source, destination, speed)
            val nextVeh = VehicleDetector.getNextVehicle(0.0, vehicle.pathManager.getNextRoads(vehicle))

            val desiredAcc = IDM.getAcceleration(vehicle, nextVeh)
            removeVehicle(vehicle)

            return desiredAcc > vehicle.comfortDeceleration
        }
    }

    private val createVehicle = object : VehicleCreationListener {
        override fun createVehicle(
            source: Waypoint,
            destination: Waypoint,
            onDespawn: RouteGeneratorDespawnListener
        ): Int? {
            // TODO: make additional checks, for example is source and destination can be connected
            // TODO: connect speed and acc paramters to conf or to veh.kt
            val nw = Vehicle.NewVehicle(
                network,
                source,
                destination,
                onDespawn,
                rnd.nextInt(4, 6) * 4.0,
                rnd.nextDouble(2.0, 2.5)
            )
            vehicles.add(nw)
            return nw.vehicleId
        }

        override fun getWaypointByJunction(junctionId: String, isStart: Boolean): Waypoint {
            val predecessors = network.roads.stream().filter {
                it.predecessor!!.getElementType().equals(ERoadLinkElementType.JUNCTION)
                    && it.predecessor!!.getElementId().equals(junctionId)
            }.toList()
            val successors = network.roads.stream().filter {
                it.successor!!.getElementType().equals(ERoadLinkElementType.JUNCTION)
                    && it.successor!!.getElementId().equals(junctionId)
            }.toList()

            if (predecessors.size + successors.size != 1) {
                throw Exception("Can't create Spawn/Despawn point from junction with id $junctionId")
            }

            if (predecessors.size == 1) {
                val road = network.getRoadById(predecessors[0].id)
                val lanes = road.lanes.filter { it.laneId == (if (isStart) -1 else 1) }.toList()
                if (lanes.size != 1) {
                    throw Exception("Can't create Spawn/Despawn point from junction with id $junctionId, don't have lane -1")
                }
                return Waypoint(
                    road.id,
                    lanes.get(0).laneId.toString(),
                )
            }

            val road = network.getRoadById(successors[0].id)
            val lanes = road.lanes.filter { it.laneId == (if (isStart) 1 else -1) }.toList()
            if (lanes.size != 1) {
                throw Exception("Can't create Spawn/Despawn point from junction with id $junctionId, don't have lane 1")
            }
            return Waypoint(
                road.id,
                lanes.get(0).laneId.toString()
            )
        }
    }
}
