package junction_intersection

import OpenDriveReader
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.backend.junction_intersection.JunctionIntersectionFinder
import kotlin.test.assertTrue

class JunctionIntersectionFinderTest {
    @Test
    fun test0() {
        val odr = OpenDriveReader()
        val openDRIVE = odr.read("xodr_for_testing.xodr")
        var finder : JunctionIntersectionFinder = JunctionIntersectionFinder(openDRIVE)
        var intersectionList = finder.findIntersection()
        println(intersectionList.size)
        for (intersect in intersectionList) {
            println("========")
            print("road1:" + intersect.roadId1 + "||lane1:" + intersect.laneId1)
            print("road2:" + intersect.roadId2 + "||lane2:" + intersect.laneId2)
            println()
        }
        val roadId1List = intersectionList.map { it.roadId1 }
        val roadId2List = intersectionList.map { it.roadId2 }
        val nonIntersectionRoads = mutableListOf("5", "6", "22", "23")

        //test that roadId1List and roadId2List aren't contain nonIntersectionRoads
        assertTrue {
            nonIntersectionRoads.all { id ->
                !roadId1List.contains(id) && !roadId2List.contains(id)
            }
        }
    }

    @Test
    fun test5() {
        val odr = OpenDriveReader()
        val openDRIVE = odr.read("xodr_for_testing.xodr")
        var finder : JunctionIntersectionFinder = JunctionIntersectionFinder(openDRIVE)
        val map = finder.junctionMap
        assertTrue {
            map.contains("0")
            map.contains("1")
            map.contains("2")
            map.contains("3")
            map.contains("4")
        }
        //junction 0
        var list : MutableList<String> = map.get("0") ?: throw NullPointerException("list of junction 0 is null")
        assertTrue {
            list.contains("5")
            list.contains("6")
        }

        //junction 1
        list = map.get("1") ?: throw NullPointerException("list of junction 1 is null")
        assertTrue {
            list.contains("2")
            list.contains("3")
            list.contains("10")
            list.contains("11")
            list.contains("12")
            list.contains("13")
            list.contains("15")
            list.contains("16")
            list.contains("17")
            list.contains("18")
            list.contains("19")
            list.contains("20")
        }

        //junction 2
        list = map.get("2") ?: throw NullPointerException("list of junction 2 is null")
        assertTrue {
            list.contains("24")
            list.contains("25")
            list.contains("27")
            list.contains("28")
            list.contains("29")
            list.contains("30")
        }

        //junction 3
        list = map.get("3") ?: throw NullPointerException("list of junction 3 is null")
        assertTrue {
            list.contains("8")
            list.contains("9")
            list.contains("31")
            list.contains("32")
            list.contains("33")
            list.contains("34")
        }

        //junction 4
        list = map.get("4") ?: throw NullPointerException("list of junction 4 is null")

        assertTrue {
            list.contains("22")
            list.contains("23")
        }
    }

    @Test
    fun test6() {
        val odr = OpenDriveReader()
        val openDRIVE = odr.read("xodr_for_testing.xodr")
        var finder : JunctionIntersectionFinder = JunctionIntersectionFinder(openDRIVE)
        val map = finder.allTRoadsInJunctionsMap
        assertTrue {
            map.contains("2")
            map.contains("3")
            map.contains("10")
            map.contains("11")
            map.contains("12")
            map.contains("13")
            map.contains("15")
            map.contains("16")
            map.contains("17")
            map.contains("18")
            map.contains("19")
            map.contains("20")

            map.contains("5")
            map.contains("6")

            map.contains("24")
            map.contains("25")
            map.contains("27")
            map.contains("28")
            map.contains("29")
            map.contains("30")

            map.contains("8")
            map.contains("9")
            map.contains("31")
            map.contains("32")
            map.contains("33")
            map.contains("34")

            map.contains("22")
            map.contains("23")

        }
    }

}
