package ru.nsu.trafficsimulator.backend.route.route_generator_new

import ru.nsu.trafficsimulator.backend.route.route_generator_new.discrete_function.Building
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val buildings = mutableListOf<Building>()
    buildings.add(Building(BuildingTypes.HOME, 3600, 3600, "1"))
    buildings.add(Building(BuildingTypes.WORK, 50, 0, "2"))
    buildings.add(Building(BuildingTypes.SHOPPING, 50, 0, "3"))
    buildings.add(Building(BuildingTypes.EDUCATION, 50, 0, "4"))
    buildings.add(Building(BuildingTypes.ENTERTAINMENT, 50, 0, "5"))

    var currentTime = 0.0
    val endTime = 1000.0
    val delta = 1.0

    val model = Model(0.0, buildings, 500)

    var travelToUpdateList : MutableList<Travel> = mutableListOf()
    while (currentTime <= endTime) {
        println("========================================================")
        println("currentTime: $currentTime")
        println("BUILDINGS")
        printBuildings(buildings)
        printTravel(travelToUpdateList)
        println("UPDATE")
        travelToUpdateList = model.call(delta, travelToUpdateList)
        println("========================================================")
        currentTime+=delta
    }

}

fun printBuildings(buildings: List<Building>) {
    for (building in buildings) {
        println(building)
    }
}

fun printTravel(travels: List<Travel>) {
    for (travel in travels) {
        println(travel)
    }
}

class ResourceReader {
    fun readTextResource(filename: String): String {
        val uri = this.javaClass.getResource("/$filename").toURI()
        return Files.readString(Paths.get(uri))
    }
}
