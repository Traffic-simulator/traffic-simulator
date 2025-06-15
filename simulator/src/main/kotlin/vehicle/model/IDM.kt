package vehicle.model

import SimulationConfig.Companion.MIN_GAP
import vehicle.Vehicle
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class IDM {



    companion object {
        private val timeHeadway = 1.6 // seconds, desired time headway
        private val sigma = 4.0

        fun getAcceleration(me: Vehicle, deltaV: Double, deltaS: Double, a_lead: Double): Double {
            val star = MIN_GAP + max(
                0.0, me.speed * timeHeadway +
                    me.speed * deltaV / sqrt(4.0 * me.maxAcc * me.comfortDeceleration)
            )

            // New term: Add leader's acceleration influence (damping factor `k`)
            val k = 0.3 // Tuning parameter (0.1-0.5)
            val accelerationTerm = k * a_lead * (deltaS / (deltaS + 1.0)) // Normalized influence

            val desiredAcc = me.maxAcc * (1 - (me.speed / me.maxSpeed).pow(sigma) - (star / deltaS).pow(2.0)) + accelerationTerm

            return max(-me.maxDeceleration, desiredAcc)
        }

        fun getAcceleration(me: Vehicle?, front: Vehicle?, deltaS: Double): Double {
            if (me == null) {
                return 0.0
            }

            if (front == null) {
                return getAcceleration(me, me.speed, deltaS, 0.0)
            }

            val deltaV = me.speed - front.speed
            return getAcceleration(me, deltaV, deltaS, front.acc)
        }

        fun getAcceleration(me: Vehicle?, front: Pair<Vehicle?, Double>): Double {
            return getAcceleration(me, front.first, front.second)
        }

        fun getStopAcceleration(me: Vehicle, deltaV: Double, deltaS: Double): Double {
            return getAcceleration(me, deltaV, deltaS, 0.0)
//            if (deltaV < 5.0 || deltaS > 100.0) {
//                return getAcceleration(me, deltaV, deltaS)
//            }
//            val bkin = deltaV * deltaV / 2.0 / deltaS
//            return - bkin * bkin / me.safeDeceleration
        }
    }
}
