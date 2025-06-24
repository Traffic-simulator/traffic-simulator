package ru.nsu.trafficsimulator.backend.path.algorithms

import ru.nsu.trafficsimulator.backend.SimulationConfig
import ru.nsu.trafficsimulator.backend.network.Lane
import ru.nsu.trafficsimulator.backend.network.Network
import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.path.Path

interface IPathBuilder {

    data class RoadWaypoint (
        val lane: Lane,
        val type: Path.PWType
    )

    fun getPath(
        source: Waypoint,
        destination: Waypoint,
        initPosition_: Double
    ): Pair<Double, List<Path.PathWaypoint>>



    // Максимальная позиция на дороге, до которой нужно перестроиться
    fun getMLCMaxRoadOffset(roadLength: Double, numLC: Int, roadLanes: Int, fromLaneId: Int): Double {
        return roadLength - numLC * SimulationConfig.MLC_MIN_DISTANCE - 15.0 * (roadLanes - Math.abs(fromLaneId))
    }

    fun isMLCPossible(length: Double, roadLanes: Int, fromLaneId_: Int, toLaneId: Int): Boolean {
        // Same logic as in path recovery, but here from and to are correct.
        var numLC = Math.abs(fromLaneId_ - toLaneId)

        if (getMLCMaxRoadOffset(length, numLC, roadLanes, fromLaneId_) < 0) {
            return false
        }
        var fromLaneId = fromLaneId_ + (if (toLaneId < fromLaneId_) -1 else 1)
        numLC--

        while(fromLaneId != toLaneId) {
            if (getMLCMaxRoadOffset(length, numLC, roadLanes, fromLaneId) < 0) {
                return false
            }

            fromLaneId = fromLaneId + (if (toLaneId < fromLaneId) -1 else 1)
            numLC--
        }
        return true
    }

    fun constructMLCReversedPathPart(from: RoadWaypoint, to: RoadWaypoint): ArrayList<Path.PathWaypoint> {
        // 1) Mandatory lane change, inside one road: have to add all lanes between with MLCMaxRoadOffset calculation
        assert(from.lane.road == to.lane.road)

        val road = to.lane.road
        val roadLanes = road.lanes.filter { it.laneId * to.lane.laneId > 0 }.size
        val fromLaneId = from.lane.laneId
        var toLaneId = to.lane.laneId

        val path = ArrayList<Path.PathWaypoint>()
        toLaneId = toLaneId + (if (fromLaneId < toLaneId) -1 else 1)
        path.add(
            Path.PathWaypoint(
                Path.PWType.MLC,
                to.lane,
                getMLCMaxRoadOffset(to.lane.length, 1, roadLanes, toLaneId)))

        var i = 2
        while(toLaneId != fromLaneId) {
            // On this step LC from tmpToLaneId to toLaneId
            val tmpFromLaneId = toLaneId + (if (fromLaneId < toLaneId) -1 else 1)
            path.add(
                Path.PathWaypoint(
                    Path.PWType.MLC,
                    road.getLaneById(toLaneId),
                    getMLCMaxRoadOffset(to.lane.length, i, roadLanes, tmpFromLaneId)))
            toLaneId = tmpFromLaneId
            i++
        }
        return path
    }
}
