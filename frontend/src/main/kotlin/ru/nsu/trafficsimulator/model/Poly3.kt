package ru.nsu.trafficsimulator.model

data class Poly3(val a: Double, val b: Double, val c: Double, val d: Double) { // x(t) = a + bx + cx^2 + dx^3
    fun value(x: Double): Double {
        return a + b * x + c * x * x + d * x * x * x
    }

    fun derivativeValue(x: Double): Double {
        return b + 2 * c * x + 3 * d * x * x
    }
}
