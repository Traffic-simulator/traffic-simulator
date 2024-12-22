package junction_intersection

import network.junction.Junction
import network.Network
import network.Road
import opendrive.OpenDRIVE
import opendrive.TRoad
import kotlin.math.pow


class JunctionIntersectionFinder(openDrive: OpenDRIVE) {


    //Map that contain road list by junction id
    val junctionMap : MutableMap<String, MutableList<TRoad>> = HashMap()
    init {
        val allRoads : MutableList<TRoad> = openDrive.road
        var junctionId : String = "-1"
        for (road : TRoad in allRoads) {
            junctionId = road.junction
            if (junctionId != "-1") {
                if (junctionMap.contains(junctionId)) {
                    val tRoadList : MutableList<TRoad>? = junctionMap[junctionId]
                    if (tRoadList == null) {
                        throw NullPointerException("List is null")
                    }
                    tRoadList.add(road)
                    junctionMap[junctionId] = tRoadList
                } else {
                    val tRoadList : MutableList<TRoad> = mutableListOf()
                    tRoadList.add(road)
                    junctionMap[junctionId] = tRoadList
                }
            }
        }
    }
//    private var allRoads : List<Road> = network.roads
//    private var allRoadsMap : MutableMap<String, Road> = HashMap()
//    init {
//    	for (road in allRoads) {
//            allRoadsMap[road.id] = road
//        }
//    }
//    fun findIntersection (junction : Junction) {
//        var indexesOfRoadsInJunction : ArrayList<String> = ArrayList<String>()
//        var junctionRoads : List<Road>
//        for (connection in junction.tjunction.connection) {
//            indexesOfRoadsInJunction.add(connection.connectingRoad);
//        }
//    }




}
