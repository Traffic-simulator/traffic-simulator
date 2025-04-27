package route_generator

import jakarta.validation.constraints.Min
import route_generator.discrete_function.DiscreteFunctionBuildings

/**
 * Класс описывающий любое здание
 */
class Building(
    @field:Min(value = 0, message = "Must be more or equals than 0") var currentPeople: Int,
    discreteFunction: DiscreteFunctionBuildings,

) {
    var name: String = ""
    //жизненно необходимо добавить поле inPath. Это люди для которых место запривачено, но мы их не можем отправить в путь.
    private var discreteFunction: DiscreteFunctionBuildings

    init {
        this.discreteFunction = discreteFunction
    }

    constructor(name: String, currentPeople: Int, discreteFunction: DiscreteFunctionBuildings) : this(
        currentPeople,
        discreteFunction,
    ) {
        this.name = name
    }

    fun changeName(name: String) {
        this.name = name
    }

    fun changePriorityFunction(discreteFunction: DiscreteFunctionBuildings) {
        this.discreteFunction = discreteFunction
    }

    val priorityFunction: DiscreteFunctionBuildings
        get() = discreteFunction

    fun changeCurrentPeople(currentPeople: Int) {
        this.currentPeople = currentPeople
    }
}
