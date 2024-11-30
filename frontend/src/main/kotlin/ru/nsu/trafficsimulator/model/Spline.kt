package ru.nsu.trafficsimulator.model

class Spline {
    val start: Point2
    val startDirection: Point2 // global coordinate
    val end: Point2
    val endDirection: Point2 // global coordinate


    constructor(
        globalStart: Point2,
        rotationRad: Double,
        length: Double,
        xPoly: Poly3,
        yPoly: Poly3,
        normalized: Boolean
    ) {
        val l = if (normalized) 1.0 else length
        this.start = globalStart + Point2(xPoly.value(0.0), yPoly.value(0.0)).rotate(rotationRad)
        this.startDirection = start + Point2(xPoly.derivativeValue(0.0), yPoly.derivativeValue(0.0)).rotate(rotationRad)
        this.end = globalStart + Point2(xPoly.value(l), yPoly.value(l)).rotate(rotationRad)
        this.endDirection = end + Point2(xPoly.derivativeValue(l), yPoly.derivativeValue(l)).rotate(rotationRad)
    }

    constructor(start: Point2, startDirection: Point2, end: Point2, endDirection: Point2) {
        this.start = start
        this.startDirection = startDirection
        this.end = end
        this.endDirection = endDirection
    }
}





