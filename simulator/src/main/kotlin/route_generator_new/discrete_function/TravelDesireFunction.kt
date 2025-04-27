package route_generator_new.discrete_function

import jakarta.validation.constraints.NotNull

/*
    Класс для функции описывающей желание человека куда-нибудь поехать в течении дня.
    Описывается 24-мя значениями, каждое значение для каждого часа.
    Значения функции имеет тип Double - ориентировочное значение 1/24.
    Если функция константно равна 1/24, то её интеграл от 0 до 24 будет равен 1 при таких значениях.
 */
class TravelDesireFunction {

    private val function: MutableList<Double>;
    companion object {
        const val NUMBER_OF_SECTIONS: Int = 24
    }

    constructor(function: @NotNull MutableList<Double>) {
        validateFunction(function)
        this.function = function
    }

    private fun validateFunction(function: MutableList<Double>) {
        require(function.isNotEmpty()) { "Function function cannot be empty." }
        require(function.size == NUMBER_OF_SECTIONS) { "Function size must be is $NUMBER_OF_SECTIONS" }
        for (y in function) {
            require(y >= 0)
        }
        // TODO подумать над валидацей, того что интеграл должен быть не больше 1.
    }

    public fun getIthNumber(i: Int) : Double {
        require(i in 0 .. NUMBER_OF_SECTIONS)
        return function[i]
    }

}
