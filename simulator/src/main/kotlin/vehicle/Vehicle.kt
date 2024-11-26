package vehicle

import SimulationConfig
import network.Lane
import vehicle.model.IDM

class Vehicle(val vehicleId: Int, var lane: Lane, val maxSpeed: Double = 33.0, val maxAcc:Double = 0.73) {

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
        val nextVeh = lane.getNextVehicle(this)
        acc = IDM.getAcceleration(this, nextVeh.first, nextVeh.second)

        speed += acc
        position += speed * deltaTime
        laneChangeTimer -= deltaTime

        while (position > lane.road.troad.length) {
            val newPosition = position - lane.road.troad.length


            // TODO: We can't go just to 0 lane with junctions.
            val nextLane = if (lane.laneId > 0) lane.successor else lane.predecessor
            if (nextLane != null && nextLane.size > 0) {
                setNewLane(nextLane[0].first)
                position = newPosition
            } else {
                // Despawn vehicle
                lane.removeVehicle(this)
                despawned = true
                break
            }
        }
    }

    fun isInLaneChange(): Boolean {
        return laneChangeTimer > 0.0
    }

    fun getLaneNumber(): Int {
        return lane.laneId;
    }

    fun setNewLane(_lane: Lane) {
        if (_lane == this.lane) return

        laneChangeTimer = SimulationConfig.LANE_CHANGE_DELAY
        this.lane.removeVehicle(this)
        this.lane = _lane
        this.lane.addVehicle(this)
    }

    companion object {
        var counter: Int =  0

        fun NewVehicle(lane: Lane, maxSpeed: Double, maxAcc: Double): Vehicle {
            return Vehicle(counter++, lane, maxSpeed, maxAcc)
        }

        // Some non standard default parameters
        fun NewVehicle(lane: Lane): Vehicle {
            return Vehicle(counter++, lane)
        }
    }

}
