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
        val openDRIVE = odr.read("only_with_param_poly3.xodr")
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
