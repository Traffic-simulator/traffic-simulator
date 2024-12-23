package junction_intersection

import opendrive.OpenDRIVE
import opendrive.TRoad


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



    fun findIntersection () : MutableList<Intersection> {
        val intersectionList : MutableList<Intersection> = ArrayList()
        var junctionId : String = "-1"
        var junctionList : MutableList<String>
        val finder : LanesIntersectionFinder = LanesIntersectionFinderImpl()
        for (pair in junctionMap) {
            junctionId = pair.key
            junctionList = pair.value
            var road1 : TRoad
            var road2 : TRoad
            for (i in 0 until junctionList.size) {
                for (j in i + 1 until junctionList.size) {
                    val road1Id = junctionList[i]
                    val road2Id = junctionList[j]
                    road1 = allTRoadsInJunctionsMap[road1Id]!!
                    road2 = allTRoadsInJunctionsMap[road2Id]!!
                    if (road1 == road2) { //find intersection between road and itself has no sense
                        continue
                    }
                    var spline1 : Spline = Spline(road1.planView.geometry[0])
                    var spline2 : Spline = Spline(road2.planView.geometry[0])
                    //TODO add normal implementation of this part
                    var lanes1 : Lanes = Lanes(road1.id, spline1, listOf(1), listOf(-1))
                    var lanes2 : Lanes = Lanes(road2.id, spline2, listOf(1), listOf(-1))
                    intersectionList.addAll(finder.findIntersections(lanes1, lanes2));
                }
            }
        }
        return intersectionList
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

}
