package vehicle.model

import network.Lane
import vehicle.Vehicle

class MOBIL {

    companion object {
        // TODO: Have to be connected with IDM and Configs
        val minimumGap = 5.0
        val bsafe = 4.0
        val ath = 0.1
        val politeness = 0.1

        fun calcAccelerationBalance(me: Vehicle, toLane: Lane): Double {

            // Check results if me will be in toLane.
            // Building whole path will be too long, so later have to do something another
            // So, calculate it temporary moving our vehicle to toLane
            me.pathBuilder.removePath(me)
            val oldLane = me.lane
            me.lane = toLane

            // New Front
            val newFront = me.pathBuilder.getNextVehicle(me, me.lane, me.direction)
            if (newFront.first?.isInLaneChange() ?: false || newFront.second < minimumGap) {
                me.pathBuilder.removePath(me)
                me.lane = oldLane
                return negativeBalance
            }
            // New Back
            val newBack = me.lane.getPrevVehicle(me)
            if (newBack.first?.isInLaneChange() ?: false || newBack.second < minimumGap) {
                me.pathBuilder.removePath(me)
                me.lane = oldLane
                return negativeBalance
            }

            // Moving to initial lane
            me.pathBuilder.removePath(me)
            me.lane = oldLane


            // Cur Front
            val curFront = me.pathBuilder.getNextVehicle(me, me.lane, me.direction)
            if (curFront.first?.isInLaneChange() ?: false) {
                return negativeBalance
            }

            val newBackNewAcc = IDM.getAcceleration(newBack.first, me, newBack.second)
            val meNewAcc = IDM.getAcceleration(me, newFront)
            if (-newBackNewAcc > bsafe || -meNewAcc > bsafe) {
                return negativeBalance
            }

            val curMeAcc = IDM.getAcceleration(me, curFront)

            val curBack = me.lane.getPrevVehicle(me)
            val curBackOldAcc = IDM.getAcceleration(curBack.first, me, curBack.second)
            val curBackNewAcc = IDM.getAcceleration(curBack.first, curFront.first, curBack.second + curFront.second + me.length)

            val newBackOldAcc = IDM.getAcceleration(newBack.first, newFront.first, newBack.second + newFront.second + me.length)

            val curBackDiffAcc = curBackNewAcc - curBackOldAcc
            val newBackDiffAcc = newBackNewAcc - newBackOldAcc
            val meDiffAcc = meNewAcc - curMeAcc

            val balance = meDiffAcc + politeness * (curBackDiffAcc + newBackDiffAcc) - ath
            return balance
        }

        val negativeBalance = -Double.MAX_VALUE
    }
}
