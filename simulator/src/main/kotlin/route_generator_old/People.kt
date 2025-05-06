package route_generator_old

import route_generator_old.discrete_function.DiscreteFunctionPeople

class People(var allPeople: Int, var inBuildingsPeople: Int, priorityFunction: DiscreteFunctionPeople) {
    private val priorityFunction: DiscreteFunctionPeople = priorityFunction

    val peopleOnTheWay: Int
        get() = allPeople - inBuildingsPeople

    fun getPriorityFunction(): DiscreteFunctionPeople {
        return priorityFunction
    }
}
