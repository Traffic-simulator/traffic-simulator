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

        // TODO: Right and Left biases not used now
        fun calcAccelerationBalance(me: Vehicle, toLane: Lane): Double {
            // TODO: check type of toLane, to not change lane to entrance lane...

            // New Front
            // TODO:
            val newFront = toLane.mockNextVehicle() // vehicle.pathBuilder.getNextVehicle(me)
            if (newFront.first?.isInLaneChange() ?: false || newFront.second < minimumGap) {
                return negativeBalance
            }

            // New Back
            val newBack = toLane.getPrevVehicle(me)
            if (newBack.first?.isInLaneChange() ?: false || newBack.second < minimumGap) {
                return negativeBalance
            }

            // Cur Front
            // TODO:
            val curFront = me.lane.mockNextVehicle() //null //me.lane.getNextVehicle(me)
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

            return meDiffAcc + politeness * (curBackDiffAcc + newBackDiffAcc) - ath
        }

        val negativeBalance = -Double.MAX_VALUE
    }
}
