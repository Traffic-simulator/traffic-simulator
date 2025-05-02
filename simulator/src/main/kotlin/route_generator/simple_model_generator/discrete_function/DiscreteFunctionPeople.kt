package route_generator.simple_model_generator.discrete_function

import jakarta.validation.constraints.NotNull

class DiscreteFunctionPeople : DiscreteFunction<Double?> {
    private val function: MutableList<Double>

    /**
     * Задает любую функцию с 24 отрезками.
     * @param function
     */
    constructor(function: @NotNull MutableList<Double>) {
        require(function.size == DiscreteFunction.NUMBER_OF_SECTIONS) { "List have size isn't 24." }
        for (i in 0 until DiscreteFunction.NUMBER_OF_SECTIONS) {
            val currentValue = function[i]
            require(!(currentValue < FUNCTION_MINIMUM_VALUE)) { "Function value is less than the minimum value." }
            require(!(currentValue > FUNCTION_MAXIMUM_VALUE)) { "Function value is greater than the maximum value." }
        }
        this.function = function
    }

    /**
     * Задает функцию с константным значением.
     * @param constanta
     */
    constructor(constanta: Double) {
        require(!(constanta < FUNCTION_MINIMUM_VALUE)) { "Function value is less than the minimum value." }
        require(!(constanta > FUNCTION_MAXIMUM_VALUE)) { "Function value is greater than the maximum value." }
        this.function = ArrayList()
        for (i in 0 until DiscreteFunction.NUMBER_OF_SECTIONS) {
            function.add(constanta)
        }
    }


    override fun getIthPriority(ithHour: Int): Double {
        return function[ithHour]
    }

    companion object {
        const val FUNCTION_MINIMUM_VALUE: Double = 0.0
        const val FUNCTION_MAXIMUM_VALUE: Double = 1.0
    }
}
