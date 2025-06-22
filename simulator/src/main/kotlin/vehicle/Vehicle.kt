package vehicle

import SimulationConfig
import SimulationConfig.Companion.JUNCTION_BLOCK_DISTANCE
import Waypoint
import mu.KotlinLogging
import network.Lane
import network.Network
import path_builder.IPathBuilder
import path_builder.DijkstraPathBuilder
import path_builder.cost_function.DynamicTimeCostFunction
import path_builder.cost_function.StaticLengthCostFunction
import route_generator.RouteGeneratorDespawnListener
import signals.SignalState
import vehicle.model.IDM

class Vehicle(
    val vehicleId: Int,
    val network: Network,
    val source: Waypoint,
    val destination: Waypoint,
    val pathBuilder: IPathBuilder,
    val despawnCallback: RouteGeneratorDespawnListener,
    val maxSpeed: Double = 30.0,
    val maxAcc: Double = 2.0
) {

    private val logger = KotlinLogging.logger("BACKEND")

    val width = 1.7
    val length = 4.5
    val comfortDeceleration = 1.67
    val safeDeceleration = 4.0
    val maxDeceleration = 9.0

    var lane = network.getLaneById(source.roadId, source.laneId)
    var direction = source.direction
    var speed = 0.0
    var acc = 0.0
    var laneChangeDistance = -0.1
    var laneChangeFullDistance = 0.0
    var laneChangeFromLaneId = 0
    var position = 1.0
    var despawned = false
    var blockingFactors = ""

    init {
        lane.addVehicle(this)
    }

    fun getLaneChangePenalty(): Double {
        val res = SimulationConfig.LANE_CHANGE_DISTANCE_GAP + speed / 1.5

        // HAVE TO BE STRICTLY SMALLER THAN MLC_MIN_DISTANCE - 10.0!!!!
        assert(res < SimulationConfig.MLC_MIN_DISTANCE)
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
                "Direction: ${direction}"
        }
    }

    // Possible problem here: in case of very close junctions vehicle can not get enough time to break before the next one,
    //  as we check only the closest one.
    // TODO: prove that mandatory lane change border starts to be stop point not only in the last road
    // TODO: vehicle all the time has to find the worst situation for braking!!!
    /*
        1) Traffic Lights (TODO: high speed walktrough!!)
        2) Junctions   (TODO: high speed walkthrough!)
        3) Other vehicles
        4) MLC
     */
    fun updateAcceleration() {
        val closestJunction = getClosestJunction()
        var junctionAcc = SimulationConfig.INF
        blockingFactors = ""
        // Rely on that traffic lights are placed in the end of each lane.
        // If lane contains red traffic light

        if (lane.signal != null && lane.signal!!.state != SignalState.GREEN) {
            // Don't block any trajectories...
            // Stop before this signal
            junctionAcc = IDM.getStopAcceleration(this, this.speed, lane.road.troad.length - position)
            logger.debug("Veh@${vehicleId} will stop because of TrafficLight@${lane.signal!!.id}")
            blockingFactors = "TrafficLight@${lane.signal!!.id}"
        } else
        if (closestJunction != null && closestJunction.distance < JUNCTION_BLOCK_DISTANCE) {
            val junction = network.getJunctionById(closestJunction.junctionId)

            // We can do that only front vehicle can block trajectories.
            // But it's dangerous in high speeds, that vehicle can not break before junction.
            // To smooth out it we can process vehicles not by distance but by predicted time to junctions.
            if (lane.getMaxPositionVehicle()!! == this) {

                // If Vehicle will get stuck on junction road it have to stop before junction to do not block it.
                // TODO: return this staff
                // val laneAfterJunction: Lane = getClosestLaneAfterJunction()!!
                // val minPosVeh = laneAfterJunction.getMinPositionVehicle()
                // val hasFreePlace: Boolean = minPosVeh == null || minPosVeh.position > 2 * MIN_GAP + minPosVeh.length
                val hasFreePlace: Boolean = true

                val tryBlockResult = junction.tryBlockTrajectoryVehicle(closestJunction.connectingRoadId, vehicleId)
                if (hasFreePlace && tryBlockResult.first) {
                    // Trajectory was succesfully blocked by us, can continue driving
                } else {
                    // Trajectory was blocked, have to stop before junction
                    junction.unlockTrajectoryVehicle(vehicleId)
                    blockingFactors = tryBlockResult.second
                    junctionAcc = IDM.getStopAcceleration(this, this.speed, closestJunction.distance)
                }
            }
        }

        // nextVehicle
        val nextVeh = pathBuilder.getNextVehicle(this)
        // nextMandatoryLaneChange
        var nextMLCDistance = pathBuilder.getNextMLCDistance(this)
        if (nextMLCDistance == null) {
            nextMLCDistance = SimulationConfig.INF
        }

        acc = minOf(
            junctionAcc,
            IDM.getAcceleration(this, nextVeh.first, nextVeh.second),
            IDM.getAcceleration(this, this.speed, nextMLCDistance, 0.0)
        )
    }

    data class ClosestJunction(val junctionId: String, val distance: Double, val connectingRoadId: String)

    // Get next junction on path, if currently on junction return the next one
    private fun getClosestJunction(): ClosestJunction? {
        var tmp_lane: IPathBuilder.PathWaypoint? = pathBuilder.getNextPathLane(this)
        var tmp_dir: Direction = direction
        var accDist = lane.road.troad.length - position

        while (tmp_lane != null && tmp_lane.lane.road.junction == "-1") {
            tmp_dir = tmp_dir.opposite(tmp_lane.isDirectionOpposite)

            accDist += tmp_lane.lane.length
            tmp_lane = pathBuilder.getNextPathLane(this, tmp_lane.lane)
        }
        if (tmp_lane == null) {
            return null
        }
        return ClosestJunction(tmp_lane.lane.road.junction, accDist, tmp_lane.lane.roadId)
    }

//    private fun getClosestLaneAfterJunction(): Lane? {
//        var tmp_lane: IPathBuilder.PathWaypoint? = pathBuilder.getNextPathLane(this)
//        var tmp_dir: Direction = direction
//
//        while (tmp_lane != null && tmp_lane.lane.road.junction == "-1") {
//            tmp_dir = tmp_dir.opposite(tmp_lane.isDirectionOpposite)
//
//            tmp_lane = pathBuilder.getNextPathLane(this, tmp_lane.lane, tmp_dir)
//        }
//        if (tmp_lane == null) {
//            return null
//        }
//        tmp_dir = tmp_dir.opposite(tmp_lane.second)
//        tmp_lane = pathBuilder.getNextPathLane(this, tmp_lane.first, tmp_dir)
//
//        if (tmp_lane == null) {
//            return null
//        }
//        return tmp_lane.first
//    }

    private fun moveToNextLane(): Boolean {
        val newPosition = position - lane.road.troad.length

        val nextLane = pathBuilder.getNextPathLane(this)
        if (nextLane != null) {
            // println("Veh moved to next lane. vehid: ${vehicleId} moved to rid: ${nextLane.first.roadId}, lid: ${nextLane.first.laneId}, olddir: ${direction}, newdir: ${if (nextLane.second) direction.opposite(direction) else direction}")

            // If was blocking junction have to unlock
            if (lane.road.junction != "-1" && nextLane.lane.road.junction != lane.road.junction) {
                network.getJunctionById(lane.road.junction).unlockTrajectoryVehicle(lane.roadId, vehicleId)
            }

            direction = direction.opposite(nextLane.isDirectionOpposite)
            setNewLane(nextLane.lane)
            position = newPosition
        } else {
            // Despawn vehicle
            logger.info { "Veh@${vehicleId} was despawned" }

            lane.removeVehicle(this)
            despawned = true
            despawnCallback.onDespawn(vehicleId)
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
        val costFunction = DynamicTimeCostFunction()
        val pathBuilder: IPathBuilder = DijkstraPathBuilder(costFunction)

        fun NewVehicle(
            network: Network,
            source: Waypoint,
            destination: Waypoint,
            despawnCallback: RouteGeneratorDespawnListener,
            maxSpeed: Double, maxAcc: Double
        ): Vehicle {
            return Vehicle(counter++, network, source, destination, pathBuilder, despawnCallback, maxSpeed, maxAcc)
        }

        fun NewVehicle(
            network: Network,
            source: Waypoint,
            destination: Waypoint,
            despawnCallback: RouteGeneratorDespawnListener
        ): Vehicle {
            return Vehicle(counter++, network, source, destination, pathBuilder, despawnCallback)
        }
    }

}
