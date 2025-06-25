package ru.nsu.trafficsimulator.backend.path.cost_function

import ru.nsu.trafficsimulator.backend.network.Lane

class StatsCostFunction : ICostFunction {

    val base = StaticLengthCostFunction()

    override fun getLaneCost(lane: Lane, time: Double): Double {
        return base.getLaneCost(lane, time) / 16.0
    }

    override fun getLaneChangeCost(numLaneChanges: Int): Double {
        return base.getLaneChangeCost(numLaneChanges)
    }

    override fun getStatLaneCost(lane: Lane, time: Double): Double {
        return getLaneCost(lane, time)
    }

    override fun getLaneAvgSpeed(lane: Lane, time: Double): Double {
        return 8.0
    }

}
