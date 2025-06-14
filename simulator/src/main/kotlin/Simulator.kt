import junction_intersection.Intersection
import junction_intersection.JunctionIntersectionFinder
import mu.KotlinLogging
import network.Lane
import network.Network
import opendrive.ERoadLinkElementType
import network.Road
import opendrive.OpenDRIVE
import path_builder.IPathBuilder
import route_generator.IRouteGenerator
import route_generator.RouteGeneratorDespawnListener
import route_generator.VehicleCreationListener
import route_generator.WaypointSpawnAbilityChecker
import route_generator_new.ModelConfig
import route_generator_new.RouteGeneratorImpl
import route_generator_new.discrete_function.Building
import route_generator_new.discrete_function.TravelDesireFunction
import vehicle.Direction
import vehicle.Vehicle
import vehicle.model.MOBIL
import java.time.LocalTime
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.random.Random

// Route - source point and destination point.
// Path - all concrete roads and lanes that vehicle will go
class Simulator(openDrive: OpenDRIVE,
                startingTime: LocalTime,
                seed: Long,
) {

    val finder = JunctionIntersectionFinder(openDrive)
    private val logger = KotlinLogging.logger("SIMULATOR")
    val intersections = finder.findIntersection()
    val network: Network = Network(openDrive.road, openDrive.junction, intersections)
    val rnd = Random(seed)
    val routeGeneratorAPI: IRouteGenerator

    val vehicles: ArrayList<Vehicle> = ArrayList()
    val buildings: List<Building>
    var currentTime: Double = startingTime.toSecondOfDay().toDouble()
    init {
        val buildingParser = BuildingsParser(openDrive)
        buildings = buildingParser.getBuildings()
        routeGeneratorAPI = RouteGeneratorImpl(currentTime, buildings)
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
        processMandatoryLaneChanges()
        processNonmandatoryLaneChanges()

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

        /*
            Vehicle generation logic.
                1) Spawn new vehicles
        */
        routeGeneratorAPI.update(dt, createVehicle, isPositionFree)

        /*
            Statistics gathering:
                1) Update segments for heatmap on this cycle
        */
        updateSegments()

        currentTime += dt
        return vehicles
    }

    // TODO: what to do if: in the end of the road vehicle use non mandatory lane change,
    //       And how to control that it has available distance to mandatory lane change after it
    //       Also timer logic makes sence
    fun processMandatoryLaneChanges() {
        vehicles.forEach { it ->

            // Vehicle will stop before closeset MLC by itself, have to just find moment and transfer it to needed lane
            // Самая дальняя позиция, где точно нужно перестроиться...
            // Может быть только перестроение на 1 полосу...
            // Перестраиваемся только на дороге длиной больше MLC_MIN_DISTANCE.
            // Начинаем задумываться о перестроении за MLC_MAX_DISTANCE.
            val laneChange: IPathBuilder.PathWaypoint? = it.pathBuilder.getNextPathLane(it)
            if (laneChange == null || laneChange.type != IPathBuilder.PWType.MLC) {
                return@forEach
            }
            assert(it.lane.road.junction == "-1")

            if (it.isInLaneChange()) {
                return@forEach
            }

            // Find possible lanes to change
            val lanesToChange = it.lane.road.lanes.filter { newLane -> abs(newLane.laneId - it.lane.laneId) == 1}
            assert(lanesToChange.contains(laneChange.lane))

            val toLane = laneChange.lane
            if (MOBIL.checkMLCAbility(it, toLane)) {
                logger.info { "Veh@${it.vehicleId} is mandatory lane changing from @${it.lane.roadId}:${it.lane.laneId} to @${toLane.roadId}:${toLane.laneId}." }
                it.pathBuilder.removePath(it)
                it.performLaneChange(toLane)
                return@forEach
            }
        }
    }

    // When doing nmlc pay respect to mls: increase initPostition and TODO: how to prevent from stupid LC?
    fun processNonmandatoryLaneChanges() {
        vehicles.forEach { it ->
            // Lane changes on junctions are prohibited
            if (it.isInLaneChange() || it.lane.road.junction != "-1") {
                return@forEach
            }

            // Find possible lanes to change
            val lanesToChange = it.lane.road.lanes.filter { newLane -> abs(newLane.laneId - it.lane.laneId) == 1}

            for (toLane in lanesToChange) {
                // Step 1: allow non-mandatory lane changes only to lanes from which the path exists.
                it.pathBuilder.removePath(it)
                val oldLane = it.lane
                it.lane = toLane
                val reachable = it.pathBuilder.isDestinationReachable(it, it.position + 2 * it.getLaneChangePenalty() + 20.0) // TODO: very bad constant, what's wrong with that?
                it.pathBuilder.removePath(it)
                it.lane = oldLane
                if (!reachable) {
                    continue
                }

                if (MOBIL.checkNMLCAbility(it, toLane)) {
                    println("Vehicle ${it.vehicleId} is non-mandatory lane changing @${it.lane.roadId}:${it.lane.laneId} to @${toLane.roadId}:${toLane.laneId}.")
                    it.pathBuilder.removePath(it)
                    it.performLaneChange(toLane)
                    return@forEach
                }
            }
        }
    }

    fun updateSegments() {
        val roads: List<Road> = network.roads
        for (road in roads) {
            for (lane in road.lanes) {
                lane.vehicles.forEach {
                    val segmentIndex = (it.position / lane.lenOfSegment).toInt()
                    if (segmentIndex < lane.segments.size) {
                        lane.segments[segmentIndex].addVehicleSpeed(it)
                    } else {
                        lane.segments.last().addVehicleSpeed(it)
                    }
                }
                lane.segments.forEach { it.update() }
//                logger.debug {
//                    "RoadId: ${lane.roadId}, LineId: ${lane.laneId}, " +
//                        "Avg by segment: ${"%.3f".format(lane.segments.map { it.currentState }.average())}, "
//                }
            }
        }
    }

    private val isPositionFree = object : WaypointSpawnAbilityChecker {
        override fun isFree(waypoint: Waypoint): Boolean {
            val lane = network.getLaneById(waypoint.roadId, waypoint.laneId)

            val minVeh = lane.getMinPositionVehicle()
            if (minVeh == null || minVeh!!.position > 30) {
                return true
            }
            return false
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

        // TODO: this logic is closer to network
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
                    if (isStart) Direction.FORWARD else Direction.BACKWARD
                )
            }

            val road = network.getRoadById(successors[0].id)
            val lanes = road.lanes.filter { it.laneId == (if (isStart) 1 else -1) }.toList()
            if (lanes.size != 1) {
                throw Exception("Can't create Spawn/Despawn point from junction with id $junctionId, don't have lane 1")
            }
            return Waypoint(
                road.id,
                lanes.get(0).laneId.toString(),
                if (isStart) Direction.BACKWARD else Direction.FORWARD
            )
        }
    }
}
