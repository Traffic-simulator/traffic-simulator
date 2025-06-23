package ru.nsu.trafficsimulator.backend.route_generator_new

import ru.nsu.trafficsimulator.backend.route_generator_new.discrete_function.BuildingTypePriorityFunction

class BuildingTypesConfig {
    companion object {
        val function = HashMap<BuildingTypes, BuildingTypePriorityFunction>()
        init {
            //TODO вынести в реальный конфиг
            val functionShopping = mutableListOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
            function.put(BuildingTypes.SHOPPING, BuildingTypePriorityFunction(functionShopping))
            val functionEducation = mutableListOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
            function.put(BuildingTypes.EDUCATION, BuildingTypePriorityFunction(functionShopping))
            val functionWork = mutableListOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
            function.put(BuildingTypes.WORK, BuildingTypePriorityFunction(functionShopping))
            val functionEntertainments= mutableListOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
            function.put(BuildingTypes.ENTERTAINMENT, BuildingTypePriorityFunction(functionShopping))
        }
    }
}
