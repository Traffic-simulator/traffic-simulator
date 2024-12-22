package junction_intersection


import OpenDriveReader
import jakarta.validation.constraints.Null
import opendrive.TRoad
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JunctionIntersectionFinderTest {
    //TODO add comparing for test 1-4 with epsilon
    @Test
    fun test() {
        val finder: SplineIntersectionFinderSectionImpl = SplineIntersectionFinderSectionImpl()
        var spline1: Spline = Spline(0.0, 3.0, -2.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/hkwypubnut
    }

    @Test
    fun test2() {
        val finder: SplineIntersectionFinderSectionImpl = SplineIntersectionFinderSectionImpl()
        var spline1: Spline = Spline(0.8, -6.1, 9.1, 10.0, -0.2, -0.1, 3.5, 3.3, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 0.5, 1.0, 3.2, -0.2, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/pv7empvtxn
    }

    @Test
    fun test3() {
        val finder: SplineIntersectionFinderSectionImpl = SplineIntersectionFinderSectionImpl()
        var spline1: Spline = Spline(0.0, 3.0, -1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/9ep9azxa2d
    }

    @Test
    fun test4() {
        val finder: SplineIntersectionFinderSectionImpl = SplineIntersectionFinderSectionImpl()
        var spline1: Spline = Spline(4.0, 3.0, -1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/gxfgqmfnz3
    }


    @Test
    fun test5() {
        val odr = OpenDriveReader()
        val openDRIVE = odr.read("only_with_param_poly3.xodr")
        var finder : JunctionIntersectionFinder = JunctionIntersectionFinder(openDRIVE)
        println(finder.junctionMap)
        for (pair in finder.junctionMap) {
            print(pair.key + ": ")
            for (road in pair.value) {
                print(road.id + " ")
            }
            println()
        }
        val map = finder.junctionMap
        assertTrue {
            map.contains("0")
            map.contains("1")
            map.contains("2")
            map.contains("3")
            map.contains("4")
        }
        //junction 0
        var list : MutableList<TRoad>? = map.get("0")
        if (list == null) {
            throw NullPointerException("list of junction 0 is null")
        }
        var listOfIds : MutableList<String> = mutableListOf()
        for (road in list) {
            listOfIds.add(road.id)
        }
        assertTrue {
            listOfIds.contains("5")
            listOfIds.contains("6")
        }

        //junction 1
        list = map.get("1")
        if (list == null) {
            throw NullPointerException("list of junction 1 is null")
        }
        listOfIds = mutableListOf()
        for (road in list) {
            listOfIds.add(road.id)
        }
        assertTrue {
            listOfIds.contains("2")
            listOfIds.contains("3")
            listOfIds.contains("10")
            listOfIds.contains("11")
            listOfIds.contains("12")
            listOfIds.contains("13")
            listOfIds.contains("15")
            listOfIds.contains("16")
            listOfIds.contains("17")
            listOfIds.contains("18")
            listOfIds.contains("19")
            listOfIds.contains("20")
        }

        //junction 2
        list = map.get("2")
        if (list == null) {
            throw NullPointerException("list of junction 2 is null")
        }
        listOfIds = mutableListOf()
        for (road in list) {
            listOfIds.add(road.id)
        }
        assertTrue {
            listOfIds.contains("24")
            listOfIds.contains("25")
            listOfIds.contains("27")
            listOfIds.contains("28")
            listOfIds.contains("29")
            listOfIds.contains("30")
        }

        //junction 3
        list = map.get("3")
        if (list == null) {
            throw NullPointerException("list of junction 3 is null")
        }
        listOfIds = mutableListOf()
        for (road in list) {
            listOfIds.add(road.id)
        }
        assertTrue {
            listOfIds.contains("8")
            listOfIds.contains("9")
            listOfIds.contains("31")
            listOfIds.contains("32")
            listOfIds.contains("33")
            listOfIds.contains("34")
        }

        //junction 4
        list = map.get("4")
        if (list == null) {
            throw NullPointerException("list of junction 4 is null")
        }
        listOfIds = mutableListOf()
        for (road in list) {
            listOfIds.add(road.id)
        }
        assertTrue {
            listOfIds.contains("22")
            listOfIds.contains("23")
        }
    }




}
