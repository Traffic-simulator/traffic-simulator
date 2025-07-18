package ru.nsu.trafficsimulator.backend.route.route_generator_new.discrete_function

import ru.nsu.trafficsimulator.backend.route.route_generator_new.BuildingTypes

class Building(
    val type: BuildingTypes,
    val capacity: Int,
    var currentPeople: Int,
    val junctionId: String) {
    override fun toString(): String {
        return "Building(type=$type, capacity=$capacity, currentPeople=$currentPeople, junctionId='$junctionId')"
    }
}
