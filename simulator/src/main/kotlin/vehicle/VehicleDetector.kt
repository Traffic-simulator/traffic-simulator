package vehicle

import network.Lane


/*
Currently we have architecture hole, because to get
closestFrontVehicle we use pathBuilder -> VehicleDetector

closestBackVehicle we use lane -> VehicleDetector
 */
class VehicleDetector {

    data class VehicleLaneSequence(val vehicle: Vehicle,
                                   val lane: Lane,
                                   val acc_distance: Double,
                                   val initial_iteration: Boolean)

    companion object {

        fun getNextVehicle(vehicle: Vehicle, laneSeq: Sequence<VehicleLaneSequence>): Pair<Vehicle?, Double> {
            var closestVehicle: Vehicle? = null

            for (it in laneSeq) {
                if (it.initial_iteration) {
                    it.lane.vehicles.forEach {
                        if (it.position > vehicle.position) {
                            if (closestVehicle == null) {
                                closestVehicle = it
                            } else {
                                if (closestVehicle!!.position > it.position) {
                                    closestVehicle = it
                                }
                            }
                        }
                    }
                    if (closestVehicle != null) {
                        val distance = closestVehicle!!.position - closestVehicle!!.length - vehicle.position
                        if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                            return Pair(null, SimulationConfig.INF)
                        }
                        return Pair(closestVehicle, distance)
                    }
                } else {
                    closestVehicle = it.lane.getMinPositionVehicle()
                }

                if (closestVehicle != null) {
                    val distance = closestVehicle!!.position - closestVehicle!!.length + it.acc_distance
                    if (distance > SimulationConfig.MAX_VALUABLE_DISTANCE) {
                        return Pair(null, SimulationConfig.INF)
                    }
                    return Pair(closestVehicle, distance)
                }
            }

            return Pair(null, SimulationConfig.INF)
        }
    }


}
