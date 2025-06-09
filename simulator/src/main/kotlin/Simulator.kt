import junction_intersection.Intersection
import junction_intersection.JunctionIntersectionFinder
import mu.KotlinLogging
import network.Network
import opendrive.ERoadLinkElementType
import network.Road
import opendrive.OpenDRIVE
import route_generator.IRouteGenerator
import route_generator.RouteGeneratorDespawnListener
import route_generator.VehicleCreationListener
import route_generator.WaypointSpawnAbilityChecker
import route_generator.random_generator.RandomRouteGenerator
import route_generator_new.RouteGeneratorImpl
import route_generator_new.Travel
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
                travelDesire: TravelDesireFunction = SimulationConfig.defaultTravelDesireDistribution) {

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
        routeGeneratorAPI = RouteGeneratorImpl(travelDesire, currentTime, buildings)
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

        // Unlock trajectories blocked by vehicles with not GREEN traffic lights
        vehicles.forEach { it.processTrafficLight() }

        processNonmandatoryLaneChanges()

        // Process vehicles in sorted order due to junction blocking logic
        // Have to sort not by position, but by distance to the closest junction...
        val sortedVehicles = vehicles.sortedBy { it.distToClosestJunction() }

        // Compute accelerations of all vehicles, needs refactor to be done in parallel
        sortedVehicles.forEach { it ->
            it.updateAcceleration()
        }

        // Apply acceleration to each vehicle with deltaTime, can be parallel
        sortedVehicles.forEach { it ->
            it.update(dt)
        }

        // despawn vehicles
        vehicles.removeAll { it.despawned == true}

        // spawn new vehicles
        routeGeneratorAPI.update(dt, createVehicle, isPositionFree)

        // Update segments for heatmap on this cycle
        updateSegments()

        currentTime += dt
        return vehicles
    }

    fun processNonmandatoryLaneChanges() {
        vehicles.forEach { it ->
            // Lane changes on junctions are prohibited
            if (it.isInLaneChange() || it.lane.road.junction != "-1") {
                return@forEach
            }

            // Find possible lanes to change
            val lanesToChange = it.lane.road.lanes.filter { newLane -> abs(newLane.laneId - it.lane.laneId) == 1}

            for (toLane in lanesToChange) {
                val balance = MOBIL.calcAccelerationBalance(it, toLane)
                if (balance > 0.0) {
                    println("Vehicle ${it.vehicleId} is lane changing ${balance}.")
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
                logger.info{
                    "RoadId: ${lane.roadId}, LineId: ${lane.laneId}, " +
                        "Avg by segment: ${"%.3f".format(lane.segments.map { it.currentState }.average())}, "
                }
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
            val nw = Vehicle.NewVehicle(
                network,
                source,
                destination,
                onDespawn,
                rnd.nextInt(5, 9) * 4.0,
                rnd.nextDouble(1.5, 2.0)
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
