package route_generator_new

import route_generator_new.discrete_function.Building
import route_generator_new.discrete_function.TravelDesireFunction
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    //1 получаю конфигурацию домов из xodr файла TODO передать рустаму какие данные мне от него нужны
    //2 инициализация файла Model.

    /*
    Матвей,
    Тебе в Model нужно вызывать только один метод call(),
    simulator/src/main/kotlin/route_generator_new/Model.kt - вот тут описал (путь от корня репы)
     */

    //var resourceReader = ResourceReader()
    //val content = resourceReader.readTextResource("travel_desire_function_const.json")
    //println(content)
    val travelDesireFunction = TravelDesireFunction(mutableListOf(0.041666666666666664, 0.041666666666666664, 0.041666666666666664, 0.041666666666666664,
        0.041666666666666664, 0.041666666666666664, 0.041666666666666664, 0.041666666666666664,
        0.041666666666666664, 0.041666666666666664, 0.041666666666666664, 0.041666666666666664,
        0.041666666666666664, 0.041666666666666664, 0.041666666666666664, 0.041666666666666664,
        0.041666666666666664, 0.041666666666666664, 0.041666666666666664, 0.041666666666666664,
        0.041666666666666664, 0.041666666666666664, 0.041666666666666664, 0.041666666666666664))
    val buildings = mutableListOf<Building>()
    buildings.add(Building(BuildingTypes.HOME, 3600, 3600, "1"))
    buildings.add(Building(BuildingTypes.WORK, 50, 0, "2"))
    buildings.add(Building(BuildingTypes.SHOPPING, 50, 0, "3"))
    buildings.add(Building(BuildingTypes.EDUCATION, 50, 0, "4"))
    buildings.add(Building(BuildingTypes.ENTERTAINMENT, 50, 0, "5"))

    var currentTime = 0.0
    val endTime = 1000.0
    val delta = 1.0

    val model = Model(travelDesireFunction, 0.0, buildings)




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
