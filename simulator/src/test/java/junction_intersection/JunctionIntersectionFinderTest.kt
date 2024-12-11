package junction_intersection

import network.Network
import org.junit.jupiter.api.Test

class JunctionIntersectionFinderTest {

    @Test
    fun test() {
        val finder: JunctionIntersectionFinder = JunctionIntersectionFinder(Network(emptyList(), emptyList()))
        var spline1: Spline = Spline(0.0, 3.0, -2.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/hkwypubnut
    }

    @Test
    fun test2() {
        val finder: JunctionIntersectionFinder = JunctionIntersectionFinder(Network(emptyList(), emptyList()))
        var spline1: Spline = Spline(0.8, -6.1, 9.1, 10.0, -0.2, -0.1, 3.5, 3.3, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 0.5, 1.0, 3.2, -0.2, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/pv7empvtxn
    }

    @Test
    fun test3() {
        val finder: JunctionIntersectionFinder = JunctionIntersectionFinder(Network(emptyList(), emptyList()))
        var spline1: Spline = Spline(0.0, 3.0, -1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/9ep9azxa2d
    }

    @Test
    fun test4() {
        val finder: JunctionIntersectionFinder = JunctionIntersectionFinder(Network(emptyList(), emptyList()))
        var spline1: Spline = Spline(4.0, 3.0, -1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var spline2: Spline = Spline(0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, PRange.NORMALIZED, 1.0)
        var result: List<Pair<Double, Double>> = finder.twoSplinesIntersection(spline1, spline2)
        println(result)
        //https://www.desmos.com/calculator/gxfgqmfnz3
    }




}
