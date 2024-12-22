package junction_intersection

import network.junction.Junction
import network.Network
import network.Road
import opendrive.OpenDRIVE
import opendrive.TRoad
import kotlin.math.pow


class JunctionIntersectionFinder(
    private val openDrive: OpenDRIVE
) {
    //TRoad's id list by junction id
    val junctionMap : MutableMap<String, MutableList<String>> = HashMap()
    //TRoad by TRoad's id (only TRoads in junction)
    val allTRoadsInJunctionsMap : MutableMap<String, TRoad> = mutableMapOf()

    //Map that contain road id list by junction id
    init {
        initJunctionMap()
        initAllTRoadsMap()
    }

    private fun initJunctionMap() {
        val allRoads : MutableList<TRoad> = openDrive.road
        var junctionId : String = "-1"
        for (road : TRoad in allRoads) {
            junctionId = road.junction
            if (junctionId != "-1") {
                if (junctionMap.contains(junctionId)) {
                    val tRoadList : MutableList<String>? = junctionMap[junctionId]
                    if (tRoadList == null) {
                        throw NullPointerException("List is null")
                    }
                    tRoadList.add(road.id)
                    junctionMap[junctionId] = tRoadList
                } else {
                    val tRoadList : MutableList<String> = mutableListOf()
                    tRoadList.add(road.id)
                    junctionMap[junctionId] = tRoadList
                }
            }
        }
    }

    private fun initAllTRoadsMap() {
        var junctionId : String = "-1"
        for (road : TRoad in openDrive.road) {
            junctionId = road.id
            if (junctionId != "-1") {
                allTRoadsInJunctionsMap[junctionId] = road
            }
        }
    }

    fun findIntersection () {
        val intersectionMap : MutableMap<String, MutableList<String>> = junctionMap
        var junctionId : String = "-1"
        var junctionList : MutableList<String> = ArrayList()
        for (pair in junctionMap) {
            junctionId = pair.key
            junctionList = pair.value
            var road1 : TRoad
            var road2 : TRoad
            for (road1Id in junctionList) {
                for (road2Id in junctionList) {

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
