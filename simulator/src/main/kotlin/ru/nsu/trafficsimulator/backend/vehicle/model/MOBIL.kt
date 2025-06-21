package ru.nsu.trafficsimulator.backend.vehicle.model

import ru.nsu.trafficsimulator.backend.SimulationConfig
import ru.nsu.trafficsimulator.backend.network.Lane
import ru.nsu.trafficsimulator.backend.vehicle.Vehicle

class MOBIL {

    companion object {
        // TODO: Have to be connected with IDM and Configs
        val bsafe = 4.0
        val ath = 0.1
        val politeness = 0.1

        fun checkNMLCAbility(me: Vehicle, toLane: Lane): Boolean {
            val accelerations = calculateAccelerations(me, toLane)
            if (accelerations == null) {
                return false
            }

            val balance = accelerations.first + politeness * (accelerations.second + accelerations.third) - ath
            return balance > 0.0
        }

        fun checkMLCAbility(me: Vehicle, toLane: Lane): Boolean {
            val accelerations = calculateAccelerations(me, toLane)
            if (accelerations == null) {
                return false
            }

            if (accelerations.first < -me.safeDeceleration) {
                return false
            }

            val balance = accelerations.first + politeness * (accelerations.second + accelerations.third)
            println("${me.vehicleId} bal $balance")
            return balance > -0.4
        }

        private fun calculateAccelerations(me: Vehicle, toLane: Lane): Triple<Double, Double, Double>? {
            val meInitAcc = me.acc

            // Check results if me will be in toLane.
            // Building whole path will be too long, so later have to do something another
            // So, calculate it temporary moving our vehicle to toLane
            me.pathManager.removePath(me)
            val oldLane = me.lane
            me.lane = toLane

            // New Front
            // TODO: check is it working that way or need to explicitly delete vehicle from lane.
            val newFront = me.getNextVehicle()
            if (newFront.first?.isInLaneChange() ?: false || newFront.second < SimulationConfig.MIN_GAP) {
                me.pathManager.removePath(me)
                me.lane = oldLane
                return null
            }
            // New Back
            val newBack = me.lane.getPrevVehicle(me)
            if (newBack.first?.isInLaneChange() ?: false || newBack.second < SimulationConfig.MIN_GAP) {
                me.pathManager.removePath(me)
                me.lane = oldLane
                return null
            }

            // Moving to initial lane
            me.pathManager.removePath(me)
            me.lane = oldLane


            // Cur Front
            val curFront = me.getNextVehicle()
            if (curFront.first?.isInLaneChange() ?: false) {
                return null
            }

            val meNewAcc = IDM.getAcceleration(me, newFront)
            me.acc = meNewAcc
            val newBackNewAcc = IDM.getAcceleration(newBack.first, me, newBack.second)
            if (-newBackNewAcc > bsafe || -meNewAcc > bsafe) {
                return null
            }
            me.acc = meInitAcc

            val curMeAcc = IDM.getAcceleration(me, curFront)

            val curBack = me.lane.getPrevVehicle(me)
            val curBackOldAcc = IDM.getAcceleration(curBack.first, me, curBack.second)
            val curBackNewAcc =
                IDM.getAcceleration(curBack.first, curFront.first, curBack.second + curFront.second + me.length)

            val newBackOldAcc =
                IDM.getAcceleration(newBack.first, newFront.first, newBack.second + newFront.second + me.length)

            val curBackDiffAcc = curBackNewAcc - curBackOldAcc
            val newBackDiffAcc = newBackNewAcc - newBackOldAcc
            val meDiffAcc = meNewAcc - curMeAcc

            return Triple(meDiffAcc, curBackDiffAcc, newBackDiffAcc)
        }

        val negativeBalance = -Double.MAX_VALUE
    }
}
