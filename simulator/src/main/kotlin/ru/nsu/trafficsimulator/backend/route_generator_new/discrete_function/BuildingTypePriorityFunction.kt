package ru.nsu.trafficsimulator.backend.route_generator_new.discrete_function

import jakarta.validation.constraints.NotNull


//TODO убрать копипаст BuildingTypePriorityFunction и TravelDesireFunction
//вынести в интерфейс
class BuildingTypePriorityFunction {
    private val function: MutableList<Int>;
    companion object {
        const val NUMBER_OF_SECTIONS: Int = 24
    }

    constructor(function: @NotNull MutableList<Int>) {
        validateFunction(function)
        this.function = function
    }

    private fun validateFunction(function: MutableList<Int>) {
        require(function.isNotEmpty()) { "Function function cannot be empty." }
        require(function.size == NUMBER_OF_SECTIONS) { "Function size must be is $NUMBER_OF_SECTIONS" }
        for (y in function) {
            require(y >= 0)
        }
    }

    public fun getIthNumber(i: Int) : Int {
        require(i in 0 .. NUMBER_OF_SECTIONS)
        return function[i]
    }
}
