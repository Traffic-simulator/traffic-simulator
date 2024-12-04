package ru.nsu.trafficsimulator.model

data class Poly3(val a: Double, val b: Double, val c: Double, val d: Double) { // x(t) = a + bt + ct^2 + dt^3
    fun value(x: Double): Double {
        return a + b * x + c * x * x + d * x * x * x
    }

    fun derivativeValue(x: Double): Double {
        return b + 2 * c * x + 3 * d * x * x
    }

    fun secondDerivativeValue(x: Double): Double {
        return 2 * c + 6 * d * x
    }

    operator fun times(x: Double): Poly3 {
        return Poly3(a * x, b * x, c * x, d * x)
    }

    operator fun plus(x: Poly3): Poly3 {
        return Poly3(a + x.a, b + x.b, c + x.c, d + x.d)
    }

    operator fun minus(x: Poly3): Poly3 {
        return Poly3(a - x.a, b - x.b, c - x.c, d - x.d)
    }
}
