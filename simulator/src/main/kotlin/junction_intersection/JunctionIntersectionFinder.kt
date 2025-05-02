package junction_intersection

import opendrive.*

class JunctionIntersectionFinder(
    private var openDrive: OpenDRIVE
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
        val finder : LanesIntersectionFinder = LanesIntersectionFinderImpl()
        for (pair in junctionMap) {
            var tRoadIDsByJunction : MutableList<String> = pair.value
            var listLength : Int = tRoadIDsByJunction.size;
            for (i in 0 until listLength) {
                for (j in i + 1 until listLength) {
                    val road1Id = tRoadIDsByJunction[i]
                    val road2Id = tRoadIDsByJunction[j]
                    var road1 : TRoad = allTRoadsInJunctionsMap[road1Id]!!
                    var road2 : TRoad = allTRoadsInJunctionsMap[road2Id]!!
                    if (road1 == road2) { //find intersection between road and itself has no sense
                        continue
                    }
                    for (geometryOfRoad1 in road1.planView.geometry) {
                        for (geometryOfRoad2 in road2.planView.geometry) {
                            var spline1 : Spline = Spline(geometryOfRoad1)
                            var spline2 : Spline = Spline(geometryOfRoad2)
                            var lanes1 : Lanes = Lanes(road1.id, spline1,
                                getListOfLanesIdPositive(road1), getListOfLanesIdNegative(road1))
                            var lanes2 : Lanes = Lanes(road2.id, spline2,
                                getListOfLanesIdPositive(road2), getListOfLanesIdNegative(road2))
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

    //https://publications.pages.asam.net/standards/ASAM_OpenDRIVE/ASAM_OpenDRIVE_Specification/latest/specification/11_lanes/11_01_introduction.html
    //here
    private fun getListOfLanesIdPositive(road : TRoad) : MutableList<Long>{
        //laneSection[0] guaranteed from front
        if (road.lanes.laneSection[0].left == null) {
            return mutableListOf();
        }
        var listOfSections : List<TRoadLanesLaneSectionLeftLane> = road.lanes.laneSection[0].left.lane
        var listOfIds : MutableList<Long> = mutableListOf()
        for (i in 0 until listOfSections.size) {
            listOfIds.add(listOfSections[i].id.toLong());
        }
        return listOfIds;
    }

    private fun getListOfLanesIdNegative(road : TRoad) : MutableList<Long>{
        //laneSection[0] guaranteed from front
        if (road.lanes.laneSection[0].right == null) {
            return mutableListOf();
        }
        var listOfSections : List<TRoadLanesLaneSectionRightLane> = road.lanes.laneSection[0].right.lane
        var listOfIds : MutableList<Long> = mutableListOf()
        for (i in 0 until listOfSections.size) {
            listOfIds.add(listOfSections[i].id.toLong());
        }
        return listOfIds;
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
