package junction_intersection

import opendrive.EParamPoly3PRange
import opendrive.TRoadPlanViewGeometry
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Spline (
    val hdg: Double = 0.0,
    val length: Double = 0.0,
    var s: Double = 0.0,
    var x: Double = 0.0,
    var y: Double = 0.0,
    val aU: Double,
    val bU: Double,
    val cU: Double,
    val dU: Double,
    val aV: Double,
    val bV: Double,
    val cV: Double,
    val dV: Double,
    val pRange: EParamPoly3PRange,
    ) {

    //TODO always reconstruct to normalized view
    //right now we are getting only normalized form
    constructor(geometry: TRoadPlanViewGeometry) :this(
        geometry.hdg, geometry.length, geometry.s, geometry.x, geometry.y,
        geometry.paramPoly3.au, geometry.paramPoly3.bu, geometry.paramPoly3.cu, geometry.paramPoly3.du,
        geometry.paramPoly3.av, geometry.paramPoly3.bv, geometry.paramPoly3.cv, geometry.paramPoly3.dv,
        pRange = geometry.paramPoly3.pRange
    )


    fun valueOfGlobal(p: Double): Pair<Double, Double> {
        val relativeCoords : Pair<Double, Double> = valueOf(p)
        return addRelativeCoords(relativeCoords)
    }



    //relative x y
    //(x, y) - return
    fun valueOf(p:Double) : Pair<Double, Double> {
        val u : Double = poly3ValueOf(aU, bU, cU, dU, p)
        val v : Double = poly3ValueOf(aV, bV, cV, dV, p)
        return rotate(Pair(u, v))
    }

    //in global coordinate
    //(x, y) - return
    fun valueOfDerivative(p : Double) : Pair<Double, Double> {
        val u : Double = poly3ValueOfDerivative(bU, cU, dU, p)
        val v : Double = poly3ValueOfDerivative(bV, cV, dV, p)
        return rotate(Pair(u, v))
    }

    private fun poly3ValueOf(a: Double, b:Double, c:Double, d:Double, p: Double) : Double {
        return a + b * p + c * p.pow(2) + d * p.pow(3)
    }

    //derivative by p  a + bp + cp^2 + dp^3
    private fun poly3ValueOfDerivative(b:Double, c:Double, d:Double, p: Double) : Double {
        return b + 2 * c * p + 3 * d * p.pow(2)
    }

    private fun rotate(coordinates: Pair<Double, Double>) : Pair<Double, Double> {
        val u = coordinates.first
        val v = coordinates.second
        val rotatedU = u * cos(hdg) - v * sin(hdg)
        val rotatedY = u * sin(hdg) + v * cos(hdg)
        return Pair(rotatedU, rotatedY)
    }

    fun getPerpendicularLeft(point : Pair<Double, Double>) : Pair<Double, Double> {
        return Pair<Double, Double> (-point.second, point.first)
    }

    fun getPerpendicularRight(point : Pair<Double, Double>) : Pair<Double, Double> {
        return Pair<Double, Double> (point.second, -point.first)
    }

    //add x y
    fun addRelativeCoords(coords: Pair<Double, Double>) : Pair<Double, Double> {
        return Pair(coords.first + x, coords.second + y)
    }

    fun normalizeVector(coords: Pair<Double, Double>): Pair<Double, Double> {
        val (x, y) = coords
        val length = sqrt(x * x + y * y)

        return if (length != 0.0) {
            Pair(x / length, y / length)
        } else {
            Pair(0.0, 0.0)
        }
    }


}
