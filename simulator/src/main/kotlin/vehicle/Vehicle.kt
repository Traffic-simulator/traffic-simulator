package vehicle

import SimulationConfig
import network.Lane
import network.Network
import path_builder.IPathBuilder
import path_builder.RandomPathBuilder
import vehicle.model.IDM

class Vehicle(val vehicleId: Int, val network: Network, var lane: Lane, var direction: Direction, val pathBuilder: IPathBuilder, val maxSpeed: Double = 33.0, val maxAcc:Double = 0.73) {

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
    var position = 10.0

    // TODO: How to handle road narrowing without junction
    fun update(deltaTime: Double) {
        println("Veh id: ${vehicleId}, Road id: ${lane.roadId}, Line id: ${lane.laneId}, Position: ${position}, Speed: ${speed}, Direction: ${direction}")

        // TODO: have to rely on PATH
        val closestJunction = getClosestJunction()
        var junctionAcc = SimulationConfig.INF

        //When close (TODO: how is depending on Block factor) to junction need block it.
        // TODO: smart distance logic... Multiple params... Depends on block type..
        val some_distance = 80.0
        if (closestJunction != null && closestJunction.distance < some_distance) {
            val junction = network.getJunctionById(closestJunction.junctionId)

            // TODO: Еще нужно следить, что ранее не блокировали, а в прочем пофиг...
            // Быть внимательным, если траектория заблокирована машиной перед нами (то есть мы можем проехать)
            // Не смотря на это заблокироваться от нас она тоже должна.
            if (junction.tryBlockTrajectoryVehicle(closestJunction.connectingRoadId, vehicleId)) {

            } else {
                junctionAcc = IDM.getAcceleration(this, this.speed, closestJunction.distance)
            }
        }

        // TODO: Not lane, have to use PathBuilder
        val nextVeh = pathBuilder.getNextVehicle(this, this.lane, this.direction, this.lane.road.troad.length - this.position)
        acc = Math.min(junctionAcc, IDM.getAcceleration(this, nextVeh.first, nextVeh.second))

        speed += acc
        speed = Math.max(speed, 0.0)
        if (speed < SimulationConfig.EPS * 10.0) {
            speed = 0.0
        }
        position += speed * deltaTime
        laneChangeTimer -= deltaTime

        while (position > lane.road.troad.length) {
            if (!moveToNextLane())
                break
        }
    }

    data class ClosestJunction(val junctionId: String, val distance: Double, val connectingRoadId: String) {

    }

    // TODO: What to do if currently on junction?
    private fun getClosestJunction(): ClosestJunction? {
        var tmp_lane: Pair<Lane, Boolean>? = pathBuilder.getNextPathLane(this)
        var tmp_dir: Direction = direction
        var accDist = lane.road.troad.length - position

        while (tmp_lane != null && tmp_lane.first.road.junction == "-1") {
            tmp_dir = tmp_dir.opposite(tmp_lane.second)

            accDist += tmp_lane.first.road.troad.length
            tmp_lane = pathBuilder.getNextPathLane(this, tmp_lane.first, tmp_dir)
        }
        if (tmp_lane == null) {
            return null
        }
        return ClosestJunction(tmp_lane.first.road.junction, accDist, tmp_lane.first.roadId)
    }

    private fun moveToNextLane(): Boolean {
        val newPosition = position - lane.road.troad.length

        val nextLane = pathBuilder.getNextPathLane(this)
        if (nextLane != null ) {
           // println("Veh moved to next lane. vehid: ${vehicleId} moved to rid: ${nextLane.first.roadId}, lid: ${nextLane.first.laneId}, olddir: ${direction}, newdir: ${if (nextLane.second) direction.opposite(direction) else direction}")

            // If was blockingJunction have to unlock
            // TODO: If connection is junc to junc?... By idea have to detect it before and block before...
            if (lane.road.junction != "-1" && nextLane.first.road.junction != lane.road.junction) {
                // TODO: is it correct roadId?
                network.getJunctionById(lane.road.junction).unlockTrajectoryVehicle(lane.roadId, vehicleId)
            }

            if (nextLane.second) {
                direction = direction.opposite(direction)
            }
            setNewLane(nextLane.first)
            position = newPosition
        } else {
            // Despawn vehicle
            println("${vehicleId} despawned")

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
        val randomPathBuilder: IPathBuilder = RandomPathBuilder(12)

        fun NewVehicle(network: Network, lane: Lane, direction: Direction, maxSpeed: Double, maxAcc: Double): Vehicle {
            return Vehicle(counter++, network, lane, direction, randomPathBuilder, maxSpeed, maxAcc)
        }

        // Some non standard default parameters
        fun NewVehicle(network: Network, lane: Lane,  direction: Direction): Vehicle {
            return Vehicle(counter++, network, lane, direction, randomPathBuilder)
        }
    }

}
