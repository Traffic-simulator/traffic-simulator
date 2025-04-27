package route_generator_new.discrete_function

import route_generator_new.BuildingTypes

class Building(
    val type: BuildingTypes,
    val capacity: Int,
    var currentPeople: Int,
    val junctionId: String) {

}
