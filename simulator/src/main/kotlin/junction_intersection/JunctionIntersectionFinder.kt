package junction_intersection

import opendrive.OpenDRIVE
import opendrive.TRoad

class JunctionIntersectionFinder(
    private val openDrive: OpenDRIVE
) {
    private val tRoadsList : List<TRoad> = openDrive.road
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
        var junctionList : MutableList<String>
        val finder : LanesIntersectionFinder = LanesIntersectionFinderImpl()
        for (pair in junctionMap) {
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
                    for (geometryOfRoad1 in road1.planView.geometry) {
                        for (geometryOfRoad2 in road2.planView.geometry) {
                            var spline1 : Spline = Spline(geometryOfRoad1)
                            var spline2 : Spline = Spline(geometryOfRoad2)
                            var lanes1 : Lanes = Lanes(road1.id, spline1, listOf(1), listOf(-1))
                            var lanes2 : Lanes = Lanes(road2.id, spline2, listOf(1), listOf(-1))
                            intersectionList.addAll(finder.findIntersections(lanes1, lanes2))
                        }
                    }
                    //var spline1 : Spline = Spline(road1.planView.geometry[0])
                    //var spline2 : Spline = Spline(road2.planView.geometry[0])
//                    //TODO add normal implementation of this part
//
                    //intersectionList.addAll(finder.findIntersections(lanes1, lanes2));
                }
            }
        }
        return intersectionList
    }

    private fun initJunctionMap() {
        var junctionId : String
        for (road : TRoad in tRoadsList) {
            junctionId = road.junction

            //if id == -1 -> TRoad not belongs to junction
            if (junctionId != "-1") {
                val tRoadList : MutableList<String> = junctionMap.getOrDefault(junctionId, mutableListOf())
                tRoadList.add(road.id)
                junctionMap[junctionId] = tRoadList;
            }
        }
    }

    private fun initAllTRoadsMap() {
        var tRoadId : String
        for (road : TRoad in tRoadsList) {
            tRoadId = road.id
            if (tRoadId != "-1") {
                allTRoadsInJunctionsMap[tRoadId] = road
            }
        }
    }
}
