package ru.nsu.trafficsimulator.backend.path.cost_function

import ru.nsu.trafficsimulator.backend.network.Lane

interface ICostFunction {

    fun getLaneCost(lane: Lane, time: Double): Double

    fun getLaneChangeCost(numLaneChanges: Int): Double

    fun getStatLaneCost(lane: Lane, time: Double): Double

    fun getLaneAvgSpeed(lane: Lane, time: Double): Double
}
