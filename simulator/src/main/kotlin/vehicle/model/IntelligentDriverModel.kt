package vehicle.model

import vehicle.Vehicle
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class IntelligentDriverModel {


        
    companion object {
        private val timeHeadway = 1.6 // seconds, desired time headway
        private val s0 = 2.0 // meters
        private val sigma = 4.0

        fun getAcceleration(me: Vehicle, deltaV: Double, deltaS: Double): Double {
            val star = s0 + max(0.0, me.speed * timeHeadway +
                    me.speed * deltaV / sqrt(4.0 * me.maxAcc * me.comfortDeceleration))

            val desiredAcc = me.maxAcc * (1 - (me.speed / me.maxSpeed).pow(sigma) - (star / deltaS).pow(2.0))

            return max(-me.maxDeceleration, desiredAcc)
        }

        fun getAcceleration(me: Vehicle, front: Vehicle, deltaS: Double): Double {
            val deltaV = me.speed - front.speed
            return getAcceleration(me, deltaV, deltaS)
        }
    }


}