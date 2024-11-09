package vehicle.model

import network.Lane
import vehicle.Vehicle

class MOBIL {


    companion object {

        // TODO: Have to be connected with IDM?
        val minimumGap = 5.0
        val bsafe = 4.0
        val ath = 0.1
        val politeness = 0.2
        val infinity: Double = 10000.0


        // TODO: Right and Left biases not used now
        fun calcAccelerationBalance(me: Vehicle, toLane: Lane): Double {
            // TODO: check type of toLane, to not change lane to entrance lane...
            var gapFront: Double = 0.0
            // TODO: Have to find vehicles on next lanes too, be careful with cars on lane delimeter
            val newFront = toLane.getNextVehicle(me)
            if (newFront != null) {
                if (newFront.isInLaneChange()) {
                    return negativeBalance
                }

                // TODO: in concurency position of newFront could be changed  after last call
                gapFront = toLane.getNextVehicleDistance(me)
                if (gapFront < minimumGap) {
                    return negativeBalance;
                }
            }

            var gapBack: Double = 0.0
            val newBack = toLane.getPrevVehicle(me)
            if (newBack != null) {
                if (newBack.isInLaneChange()) {
                    return negativeBalance
                }

                // TODO: in concurency position of newFront could be changed  after last call
                gapBack = toLane.getPrevVehicleDistance(me)
                // TODO: I think have to add + T * v
                if (gapBack < minimumGap) {
                    return negativeBalance
                }
            }

            val curFront = me.lane.getNextVehicle(me)
            var curFrontDist = 10000.0
            if (curFront != null) {
                if (curFront.isInLaneChange()) {
                    return negativeBalance
                }


                curFrontDist = me.lane.getNextVehicleDistance(me)
            }

            val newBackNewAcc: Double = if (newBack == null) 0.0 else IntelligentDriverModel.getAcceleration(newBack, me, gapBack)
            val meNewAcc: Double = if (newFront == null) IntelligentDriverModel.getAcceleration(me, 0.0, 10000.0)
            else IntelligentDriverModel.getAcceleration(me, newFront, gapFront)

            if (-newBackNewAcc > bsafe || -meNewAcc > bsafe) {
                return negativeBalance
            }

            var curMeAcc: Double = 0.0
            if (curFront == null) {
                curMeAcc = IntelligentDriverModel.getAcceleration(me, me.speed, infinity)
            } else {
                curMeAcc = IntelligentDriverModel.getAcceleration(me, curFront, curFrontDist)
            }

            // TODO:
            val curBackOldAcc = 0.0
            val curBackNewAcc = 0.0

            // TODO: We can't do that when multiple roads!!!
            var newBackOldAcc: Double = 0.0
            if (newBack != null) {
                if (newFront != null) {
                    newBackOldAcc = IntelligentDriverModel.getAcceleration(newBack, newFront, newFront.position - newBack.position)
                } else {
                    newBackOldAcc = IntelligentDriverModel.getAcceleration(newBack, newBack.speed, infinity)
                }
            }

            val curBackDiffAcc = curBackNewAcc - curBackOldAcc
            val newBackDiffAcc = newBackNewAcc - newBackOldAcc
            val meDiffAcc = meNewAcc - curMeAcc

            return meDiffAcc + politeness * (curBackDiffAcc + newBackDiffAcc) - ath
        }


        val negativeBalance = -Double.MAX_VALUE
    }
}