package ru.nsu.trafficsimulator.backend.path.cost_function

import ru.nsu.trafficsimulator.backend.network.Lane
import kotlin.math.sign

class DynamicTimeCostFunction: ICostFunction {

    // Returns average time to pass lane
    // Actually lane changes broke this logic a bit,
    // because we can pass road by parts on different lanes
    // TODO: somehow сonsider road parts
    //  1) trivial solution - calculate avg speed on road-side instead of lane
    override fun getLaneCost(lane: Lane): Double {
        val laneAvgSpeed = lane.road.getAverageRoadSideSpeed(lane.laneId.sign)
        if (laneAvgSpeed < 1) return lane.length
        return lane.length / laneAvgSpeed
    }

    override fun getLaneChangeCost(numLaneChanges: Int): Double {
        return 10.0 * numLaneChanges
    }
}
