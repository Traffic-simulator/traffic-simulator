package path_builder.cost_function

import network.Lane

interface ICostFunction {

    fun getLaneCost(lane: Lane): Double

    fun getLaneChangeCost(numLaneChanges: Int): Double
}
