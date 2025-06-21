package ru.nsu.trafficsimulator.backend.path.cost_function

import ru.nsu.trafficsimulator.backend.network.Lane

interface ICostFunction {

    fun getLaneCost(lane: Lane): Double

    fun getLaneChangeCost(numLaneChanges: Int): Double
}
