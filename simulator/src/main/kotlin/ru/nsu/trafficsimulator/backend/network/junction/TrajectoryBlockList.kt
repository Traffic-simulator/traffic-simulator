package ru.nsu.trafficsimulator.backend.network.junction

import ru.nsu.trafficsimulator.backend.junction_intersection.Intersection
import ru.nsu.trafficsimulator.backend.network.Network
import opendrive.TJunctionConnection

class TrajectoryBlockList(me: TJunctionConnection, others: List<TJunctionConnection>, val intersections: MutableList<Intersection>, val network: Network) {

    val blockList: List<TJunctionConnection>

    init {
        val filtered = intersections.filter { it.roadId1 == me.connectingRoad || it.roadId2 == me.connectingRoad }.toList()
        blockList = others.filter { otherConn ->
            filtered.find { (it.roadId1 == otherConn.connectingRoad || it.roadId2 == otherConn.connectingRoad) && otherConn.connectingRoad != me.connectingRoad} != null
        }.filter {

            val itConnectingRoad = network.getRoadById(it.connectingRoad)
            val meConnectingRoad = network.getRoadById(me.connectingRoad)

            assert(itConnectingRoad.numLanes == 1)
            assert(meConnectingRoad.numLanes == 1)

            // Here we probably have to take succesor in case of wrong direction
            val itLanePredecessors = itConnectingRoad.lanes.get(0).predecessor
            val meLanePredecessors = meConnectingRoad.lanes.get(0).predecessor

            assert(itLanePredecessors!!.size == 1)
            assert(meLanePredecessors!!.size == 1)

            itLanePredecessors.get(0).first != meLanePredecessors.get(0).first
        }
        val flag = true
        // TODO: filter not by incoming road but by incoming lane...
        // AND block junction not by distance but by time to junction or by time to and with junction
    }
}
