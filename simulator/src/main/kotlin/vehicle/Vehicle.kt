package vehicle

import network.Lane
import vehicle.model.IntelligentDriverModel

class Vehicle(var lane: Lane, val maxSpeed: Double = 33.0, val maxAcc:Double = 0.73) {

    val width = 1.7
    val length = 4.5
    val comfortDeceleration = 1.67
    val safeDeceleration = 4.0
    val maxDeceleration = 9.0
    var speed = 0.0
    var acc = 0.0
    var laneChangeTimer = 0.0


    init {
        assert(lane != null)
        lane.addVehicle(this)
    }

    var position = 0.0 // position of front bumper in road length. Vehicle is going to the next road if the back bumper is not on the road

    fun update(deltaTime: Double) {
        val nextVeh = lane.getNextVehicle(this)
        if (nextVeh == null) {
            acc = IntelligentDriverModel.getAcceleration(this, speed, 10000.0)
        } else {
            acc = IntelligentDriverModel.getAcceleration(this, nextVeh, nextVeh.position - position)
        }
        speed += acc
        position += speed * deltaTime
        laneChangeTimer -= deltaTime
    }

    fun isInLaneChange(): Boolean {
        return laneChangeTimer > 0.0
    }

    fun getLaneNumber(): Int {
        return lane.laneId;
    }

    fun setNewLane(_lane: Lane) {
        if (_lane == this.lane) return

        laneChangeTimer = 5.0
        this.lane.vehicles.remove(this)
        this.lane = _lane
        this.lane.vehicles.add(this)
    }

    companion object {
        fun NewVehicle(lane: Lane, maxSpeed: Double, maxAcc: Double): Vehicle {
            return Vehicle(lane, maxSpeed, maxAcc)
        }

        // Some non standard default parameters
        fun NewVehicle(lane: Lane): Vehicle {
            return Vehicle(lane)
        }
    }

}