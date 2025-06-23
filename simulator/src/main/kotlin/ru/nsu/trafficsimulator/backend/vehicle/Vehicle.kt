package ru.nsu.trafficsimulator.backend.vehicle

import mu.KotlinLogging
import ru.nsu.trafficsimulator.backend.SimulationConfig
import ru.nsu.trafficsimulator.backend.SimulationConfig.Companion.JUNCTION_BLOCK_DISTANCE
import ru.nsu.trafficsimulator.backend.Simulator
import ru.nsu.trafficsimulator.backend.network.Lane
import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.Path
import ru.nsu.trafficsimulator.backend.path.PathManager
import ru.nsu.trafficsimulator.backend.path.algorithms.CachedDijkstraPathBuilder
import ru.nsu.trafficsimulator.backend.path.cost_function.DynamicTimeCostFunction
import ru.nsu.trafficsimulator.backend.path.cost_function.ICostFunction
import ru.nsu.trafficsimulator.backend.route.RouteGeneratorDespawnListener
import ru.nsu.trafficsimulator.backend.vehicle.model.IDM
import ru.nsu.trafficsimulator.backend.vehicle.model.MOBIL
import signals.SignalState
import kotlin.math.abs
import kotlin.math.min

class Vehicle(
    val vehicleId: Int,
    val network: Network,
    val source: Waypoint,
    val destination: Waypoint,
    val pathManager: PathManager,
    val despawnCallback: RouteGeneratorDespawnListener?,
    val globalMaxSpeed: Double = 30.0,
    val maxAcc: Double = 2.0,
    var speed: Double = 0.0
) {

    private val logger = KotlinLogging.logger("BACKEND")

    val width = 1.7
    val length = 4.5
    val comfortDeceleration = 1.67
    val safeDeceleration = 4.0
    val maxDeceleration = 9.0

    var lane = network.getLaneById(source.roadId, source.laneId)
    var acc = 0.0
    var laneChangeDistance = -0.1
    var laneChangeFullDistance = 0.0
    var laneChangeFromLaneId = 0
    var position = 0.0
    var despawned = false
    var distToClosestJunctionTmp: Double = SimulationConfig.INF
    var blockingFactors = ""
    var maxSpeed = globalMaxSpeed

    init {
        lane.addVehicle(this)
    }

    fun getLaneChangePenalty(): Double {
        val res = SimulationConfig.LANE_CHANGE_DISTANCE_GAP + speed
        assert(res < SimulationConfig.MLC_MIN_DISTANCE - 10.0)
        return res
    }

    // Have to be called only after updateAcceleration!!!
    fun update(deltaTime: Double) {
        speed += acc * deltaTime
        speed = Math.max(speed, 0.0)
        if (speed < SimulationConfig.EPS) {
            speed = 0.0
        }
        val deltaPos = speed * deltaTime
        position += deltaPos
        laneChangeDistance -= deltaPos

        while (position > lane.road.troad.length) {
            if (!moveToNextLane())
                break
        }
        logger.debug {
            "Veh@$vehicleId, RoadId: ${lane.roadId}, LineId: ${lane.laneId}, " +
                "Position: ${"%.3f".format(position)}, " +
                "Speed: ${"%.3f".format(speed)}, " +
                "Acceleration: ${"%.3f".format(acc)}, " +
                "Direction: ${lane.direction}"
        }
    }

    // TODO: in case of very close junctions vehicle can not get enough time to break before the next one, as we check only the closest one.
    /*
        1) Traffic Lights (TODO: high speed walktrough!!)
        2) Junctions
        3) Other vehicles
        4) MLC
     */
    fun updateAcceleration() {
        val closestJunction = getClosestJunction()
        var junctionAcc     = SimulationConfig.INF
        blockingFactors = ""

        // Checking traffic lights
        if (lane.signal != null && lane.signal!!.state != SignalState.GREEN) {
            // Don't block any trajectories...
            // Stop before this signal
            junctionAcc = IDM.getStopAcceleration(this, this.speed, lane.road.troad.length - position)
            logger.debug("Veh@${vehicleId} will stop because of TrafficLight@${lane.signal!!.id}")
            blockingFactors = "TrafficLight@${lane.signal!!.id}"
        } else
        // There is no blocking traffic light, continue...
        if (closestJunction != null && closestJunction.distance < JUNCTION_BLOCK_DISTANCE) {
            val junction = network.getJunctionById(closestJunction.junctionId)

            // We can do that only front vehicle can block trajectories.
            // But it's dangerous in high speeds, that vehicle can not break before junction.
            // To smooth out it we can process vehicles not by distance but by predicted time to junctions.
            if (lane.getMaxPositionVehicle()!! == this && lane.road.junction == "-1") {

                // If Vehicle will get stuck on junction road it have to stop before junction to do not block it.
                val freePlace = getFreePlaceAfterJunction()
                val tryBlockResult = junction.tryBlockTrajectoryVehicle(closestJunction.connectingRoadId, vehicleId)
                if (freePlace > SimulationConfig.EPS && tryBlockResult.first) {
                    // Trajectory was succesfully blocked by us, can continue driving
                    // TODO: calc junctionAcc to the next junction
                } else {
                    // Trajectory was blocked, have to stop before junction
                    junction.unlockTrajectoryVehicle(vehicleId)
                    blockingFactors = tryBlockResult.second
                    junctionAcc = IDM.getStopAcceleration(this, this.speed, closestJunction.distance)
                }
            } else {
                junctionAcc = IDM.getStopAcceleration(this, this.speed, closestJunction.distance)
            }
        }

        // Calc acceleration
        maxSpeed = min(globalMaxSpeed, lane.getMaxSpeed())
        val nextVeh = getNextVehicle()
        val nextMLCDistance = pathManager.getNextMLCDistance(this)

        acc = minOf(
            junctionAcc,
            IDM.getAcceleration(this, nextVeh.first, nextVeh.second),
            IDM.getAcceleration(this, this.speed, nextMLCDistance, 0.0)
        )
    }

    fun getNextVehicle(): Pair<Vehicle?, Double> {
        return VehicleDetector.getNextVehicle(position, pathManager.getNextRoads(this))
    }

    data class ClosestJunction(val junctionId: String, val distance: Double, val connectingRoadId: String)

    // Get next junction on path, if currently on junction return the next one
    private fun getClosestJunction(): ClosestJunction? {
        var tmp_lane: Path.PathWaypoint? = pathManager.getNextPathLane(this)
        var accDist = lane.road.troad.length - position

        while (tmp_lane != null && tmp_lane.lane.road.junction == "-1") {
            accDist += tmp_lane.lane.length
            tmp_lane = pathManager.getNextPathLane(this, tmp_lane.lane)
        }
        if (tmp_lane == null) {
            return null
        }
        return ClosestJunction(tmp_lane.lane.road.junction, accDist, tmp_lane.lane.roadId)
    }

    private fun getFreePlaceAfterJunction(): Double {
        // We are the first car in front of junction and in the not junction road
        var tmp_lane = pathManager.getNextPathLane(this)
        if (tmp_lane == null) {
            return SimulationConfig.INF
        }
        var occupiedSpace = 0.0
        tmp_lane.lane.vehicles.forEach { occupiedSpace += it.length + SimulationConfig.MIN_GAP }

        assert(tmp_lane.lane.road.junction != "-1")


        tmp_lane = pathManager.getNextPathLane(this, tmp_lane.lane)
        if (tmp_lane == null) {
            return SimulationConfig.INF
        }

        val minPosVeh = tmp_lane.lane.getMinPositionVehicle()
        if (minPosVeh == null) {
            return tmp_lane.lane.length - occupiedSpace
        }
        return minPosVeh.position - minPosVeh.length - SimulationConfig.MIN_GAP - occupiedSpace
    }


    // TODO: what to do if: in the end of the road vehicle use non mandatory lane change,
    //       And how to control that it has available distance to mandatory lane change after it
    //       Also timer logic makes sence
    fun processMLC() {
        val laneChange = pathManager.getNextPathLane(this)
        if (laneChange == null || laneChange.type != Path.PWType.MLC) {
            return
        }
        assert(lane.road.junction == "-1")

        if (isInLaneChange()) {
            return
        }

        // Find possible lanes to change
        val lanesToChange = lane.road.lanes.filter { newLane -> abs(newLane.laneId - lane.laneId) == 1}
        assert(lanesToChange.contains(laneChange.lane))

        val toLane = laneChange.lane
        if (MOBIL.checkMLCAbility(this, toLane)) {
            logger.info { "Veh@$vehicleId is mandatory lane changing from @${lane.roadId}:${lane.laneId} to @${toLane.roadId}:${toLane.laneId}." }
            pathManager.removePath(this)
            performLaneChange(toLane)
        }
    }

    // When doing nmlc pay respect to mls: increase initPostition and TODO: how to prevent from stupid LC?
    fun processNMLC() {
        // Lane changes on junctions are prohibited
        if (isInLaneChange() || lane.road.junction != "-1") {
            return
        }

        // Find possible lanes to change
        val lanesToChange = lane.road.lanes.filter { newLane -> abs(newLane.laneId - lane.laneId) == 1}

        for (toLane in lanesToChange) {
            // Step 1: allow non-mandatory lane changes only to lanes from which the path exists.
            pathManager.removePath(this)
            val oldLane = lane
            lane = toLane
            val reachable = pathManager.isDestinationReachable(this, this.position + 2 * this.getLaneChangePenalty() + 20.0) // TODO: very bad constant, what's wrong with that?
            pathManager.removePath(this)
            lane = oldLane
            if (!reachable) {
                continue
            }

            if (MOBIL.checkNMLCAbility(this, toLane)) {
                pathManager.removePath(this)
                performLaneChange(toLane)
                return
            }
        }
    }

    private fun moveToNextLane(): Boolean {
        val newPosition = position - lane.road.troad.length

        val nextLane = pathManager.getNextPathLane(this)
        if (nextLane != null) {

            // If was blocking junction have to unlock
            if (lane.road.junction != "-1" && nextLane.lane.road.junction != lane.road.junction) {
                network.getJunctionById(lane.road.junction).unlockTrajectoryVehicle(lane.roadId, vehicleId)
            }

            setNewLane(nextLane.lane)
            position = newPosition
            maxSpeed = min(globalMaxSpeed, nextLane.lane.getMaxSpeed())
        } else {
            // Despawn vehicle
            logger.debug { "Veh@${vehicleId} was despawned" }

            lane.removeVehicle(this)
            despawned = true
            network.junctions.forEach{ it.unlockTrajectoryVehicle(vehicleId) }
            despawnCallback?.onDespawn(vehicleId)
            return false
        }
        return true
    }

    fun processTrafficLight() {
        // Возможно сейчас данная машина блокирует некоторые траектории.
        // В качестве тупой реализации просто проверим вообще все
        val closestJunction = getClosestJunction()
        if (closestJunction != null && lane.signal != null && lane.signal!!.state != SignalState.GREEN) {
            val junction = network.getJunctionById(closestJunction.junctionId)
            junction.unlockTrajectoryVehicle(vehicleId = vehicleId)
        }
    }

    fun isInLaneChange(): Boolean {
        return laneChangeDistance > 0.0
    }

    fun getLaneNumber(): Int {
        return lane.laneId;
    }

    private fun setNewLane(_lane: Lane) {
        if (_lane == this.lane) return

        this.lane.removeVehicle(this)
        this.lane = _lane
        this.lane.addVehicle(this)
    }

    fun performLaneChange(_lane: Lane) {
        if (_lane == this.lane) return

        laneChangeDistance = getLaneChangePenalty()
        laneChangeFullDistance = laneChangeDistance
        laneChangeFromLaneId = lane.laneId
        // TODO: We don't need to traverse all junction, only the closest one...
        network.junctions.forEach{ it.unlockTrajectoryVehicle(vehicleId) }
        setNewLane(_lane)
    }

    fun distToClosestJunction(): Double {
        val closestJunction = getClosestJunction()
        if (closestJunction != null) {
            return closestJunction.distance
        }

        return SimulationConfig.INF
    }

    companion object {
        var counter: Int = 0
        lateinit var costFunction: ICostFunction
        lateinit var pathManager: PathManager

        fun initialize(network: Network, simulator: Simulator) {
            costFunction = DynamicTimeCostFunction()
            pathManager = PathManager(CachedDijkstraPathBuilder(network, simulator, costFunction, 20.0))
            // pathManager = PathManager(DijkstraPathBuilder(network, costFunction))
        }


        fun NewVehicle(
            network: Network,
            source: Waypoint,
            destination: Waypoint,
            despawnCallback: RouteGeneratorDespawnListener,
            maxSpeed: Double, maxAcc: Double, speed: Double = 0.0
        ): Vehicle {
            // This if is not correct when region simulation
//             if (source.roadId == destination.roadId) {
//                throw RuntimeException("WTF, broooo!??")
//             }
            return Vehicle(counter++, network, source, destination, pathManager, despawnCallback, maxSpeed, maxAcc, speed)
        }

        fun createTempVehicle(
            network: Network,
            source: Waypoint,
            destination: Waypoint,
            speed: Double
        ): Vehicle {
            return Vehicle(-1, network, source, destination, pathManager, null, speed, 2.0, speed)
        }

        fun NewVehicle(
            network: Network,
            source: Waypoint,
            destination: Waypoint,
            despawnCallback: RouteGeneratorDespawnListener
        ): Vehicle {
            return Vehicle(counter++, network, source, destination, pathManager, despawnCallback)
        }
    }

}
