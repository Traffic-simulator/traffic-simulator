package path_builder.cost_function

import network.Lane

class StaticLengthCostFunction : ICostFunction {
    override fun getLaneCost(lane: Lane): Double {
        return lane.length
    }

    override fun getLaneChangeCost(numLaneChanges: Int): Double {
        return 100.0 * numLaneChanges
    }

}
