package route_generator_new.discrete_function

import jakarta.validation.constraints.NotNull

/*
    Класс для функции описывающей желание человека куда-нибудь поехать в течении дня.
    Описывается 24-мя значениями, каждое значение для каждого часа.
    Значения функции имеет тип Long - это 1/5184000.
    Если функция константно равна 1, то её интеграл от 0 до 24 будет равен 1 при таких значениях.
 */
class TravelDesireFunction {

    private val function: MutableList<Long>;
    companion object {
        const val NUMBER_OF_SECTIONS: Int = 24
        const val NUMBER_OF_FRAMES_IN_DAY = 5184000//24 часа * 60 минут * 60 секунд * 60 кадров
    }

    constructor(function: @NotNull MutableList<Long>) {
        validateFunction(function)
        this.function = function
    }

    private fun validateFunction(function: MutableList<Long>) {
        require(function.isNotEmpty()) { "Function function cannot be empty." }
        require(function.size == NUMBER_OF_SECTIONS) { "Function size must be is $NUMBER_OF_SECTIONS" }
        for (y in function) {
            require(y >= 0)
        }
        // TODO подумать над валидацей, того что интеграл должен быть не больше 1.
    }


}
