package ru.nsu.trafficsimulator.backend.path.cost_function

import ru.nsu.trafficsimulator.backend.network.Lane

class StaticLengthCostFunction : ICostFunction {
    override fun getLaneCost(lane: Lane): Double {
        return lane.length
    }

    override fun getLaneChangeCost(numLaneChanges: Int): Double {
        return 100.0 * numLaneChanges
    }

    override fun getStatLaneCost(lane: Lane, secondsOfDay: Double): Double {
        TODO("Not yet implemented")
    }

}
