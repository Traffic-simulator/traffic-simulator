package vehicle

import SimulationConfig
import network.Lane
import network.Network
import vehicle.model.IDM

class Vehicle(val vehicleId: Int, val network: Network, var lane: Lane, var direction: Direction, val maxSpeed: Double = 33.0, val maxAcc:Double = 0.73) {

    val width = 1.7
    val length = 4.5
    val comfortDeceleration = 1.67
    val safeDeceleration = 4.0
    val maxDeceleration = 9.0
    var speed = 0.0
    var acc = 0.0
    var laneChangeTimer = 0.0

    var despawned = false

    init {
        lane.addVehicle(this)
    }

    // position of front bumper in road length. Vehicle is going to the next road if the front bumper is not on the road
    var position = 0.0

    // TODO: How to handle road narrowing without junction
    fun update(deltaTime: Double) {
        // TODO: have to rely on PATH
        val closestJunction = getClosestJunction()
        var junctionAcc = SimulationConfig.INF


        //When close (TODO: how is depending on Block factor) to junction need block it.
        // TODO: smart distance logic... Multiple params... Depends on block type..
        val some_distance = 200.0
        if (closestJunction != null && closestJunction.second < some_distance) {
            val junction = network.getJunctionById(closestJunction.first)

            // TODO: Еще нужно следить, что ранее не блокировали, а в прочем пофиг...
            // Быть внимательным, если траектория заблокирована машиной перед нами (то есть мы можем проехать)
            // Не смотря на это заблокироваться от нас она тоже должна.
            if (junction.tryBlockTrajectoryVehicle(closestJunction.third, vehicleId)) {

            } else {
                junctionAcc = IDM.getAcceleration(this, this.speed, closestJunction.second)
            }
        }

        val nextVeh = lane.getNextVehicle(this)
        acc = Math.min(junctionAcc, IDM.getAcceleration(this, nextVeh.first, nextVeh.second))

        speed += acc
        position += speed * deltaTime
        laneChangeTimer -= deltaTime

        while (position > lane.road.troad.length) {
            if (!moveToNextLane())
                break
        }
    }

    // Returns JunctionId, DistanceToJunction, ConnectorRoadId
    // TODO: What to do if currently on junction?
    private fun getClosestJunction(): Triple<String, Double, String>? {
        // TODO: Have to rely on path - maybe even PathModule, but now just random or 0.
        // TODO: Don't we need to copy?

        var tmp_lane: Lane? = lane.getNextLane(direction)?.first
        var accDist = lane.road.troad.length - position
        while (tmp_lane != null && tmp_lane.road.junction == "-1") {
            accDist += tmp_lane.road.troad.length
            tmp_lane = tmp_lane.getNextLane(direction)?.first
        }
        if (tmp_lane == null) {
            return null
        }
        return Triple(tmp_lane.road.junction, accDist, tmp_lane.roadId)
    }

    private fun moveToNextLane(): Boolean {
        val newPosition = position - lane.road.troad.length

        val nextLaneList = if (direction == Direction.FORWARD) lane.successor else lane.predecessor
        if (nextLaneList != null && nextLaneList.size > 0) {

            // TODO: We can't go just to 0 lane with junctions.
            val nextLane = nextLaneList.get(0)

            // If was blockingJunction have to unlock
            // TODO: If connection is junc to junc?... By idea have to detect it before and block before...
            if (lane.road.junction != "-1" && nextLane.road.junction != lane.road.junction) {
                // TODO: is it correct roadId?
                network.getJunctionById(lane.road.junction).unlockTrajectoryVehicle(lane.roadId, vehicleId)
            }

            setNewLane(nextLane)
            position = newPosition
        } else {
            // Despawn vehicle
            lane.removeVehicle(this)
            despawned = true
            return false
        }
        return true
    }

    fun isInLaneChange(): Boolean {
        return laneChangeTimer > 0.0
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

        laneChangeTimer = SimulationConfig.LANE_CHANGE_DELAY
        setNewLane(_lane)
    }

    companion object {
        var counter: Int =  0

        fun NewVehicle(network: Network, lane: Lane, direction: Direction, maxSpeed: Double, maxAcc: Double): Vehicle {
            return Vehicle(counter++, network, lane, direction, maxSpeed, maxAcc)
        }

        // Some non standard default parameters
        fun NewVehicle(network: Network, lane: Lane,  direction: Direction): Vehicle {
            return Vehicle(counter++, network, lane, direction)
        }
    }

}
