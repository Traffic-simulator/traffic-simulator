package ru.nsu.trafficsimulator.backend.route.route_generator_new

import ru.nsu.trafficsimulator.backend.route.route_generator_new.discrete_function.Building

/**
 * Класс является вспомогательным для отслеживания информации о домах.
 */
class Homes {
    private val homes: HashMap<String, Building> = hashMapOf()
    private val numberOfAllHumans : Int
    private var numberOfHumansInHomes : Int

    //TODO вынести в отдельный класс эти три списка
    private val emptyHomes: HashMap<String, Building> = hashMapOf()
    private val nonEmptyHomes: HashMap<String, Building> = hashMapOf()
    private val fullHomes: HashMap<String, Building> = hashMapOf()

    constructor(buildings: List<Building>) {
        var currentPeople = 0
        for(building in buildings) {
            if (building.type == BuildingTypes.HOME) {
                homes.put(building.junctionId, building)
                fullHomes.put(building.junctionId, building)
                nonEmptyHomes.put(building.junctionId, building)

                currentPeople+=building.currentPeople
            }
        }
        numberOfAllHumans = currentPeople
        numberOfHumansInHomes = numberOfAllHumans
    }

    // Возвращает коль-во людей в пути(не в доме(buildingtype=home))
    fun getNumberOfHumansOnWay() : Int {
        return numberOfAllHumans - numberOfHumansInHomes
    }

    //метод добавляет человека к текущему зданию
    fun increaseNumberOfPeopleInHome(junctionId: String) {
        val building : Building = homes[junctionId]!!
        //обновляем fullHomes в случае заполнения
        if (building.currentPeople == building.capacity - 1) {
            fullHomes.put(building.junctionId, building)
        }
        //обновляем emptyHomes и nonEmptyHomes в случае добавления туда человека
        if (building.currentPeople == 0) {
            emptyHomes.remove(building.junctionId)
            nonEmptyHomes.put(building.junctionId, building)
        }
        building.currentPeople++
    }

    //метод удаляет человека из здания
    fun decreaseNumberOfHumansInHome(junctionId: String) {
        val building : Building = homes[junctionId]!!
        //обновляем fullHomes в случае удаления от туда человека
        if (building.currentPeople == building.capacity) {
            fullHomes.remove(building.junctionId)
        }
        //обновляем emptyHomes nonEmptyHomes в случае если здание опустело
        if (building.currentPeople == 1) {
            nonEmptyHomes.remove(building.junctionId)
            emptyHomes.put(building.junctionId, building)
        }
        building.currentPeople--
    }

    fun getNonEmpty() : HashMap<String, Building> = nonEmptyHomes


    fun decreaseNumberOfHumansInHomes(diff : Int) {
        numberOfHumansInHomes -= diff
        require(numberOfHumansInHomes >= 0)
    }

    fun increaseNumberOfHumansInHomes(diff : Int) {
        numberOfHumansInHomes += diff
        require(numberOfHumansInHomes <= numberOfAllHumans)
    }

    fun getNumberOfHumansInHomes() : Int {
        return numberOfHumansInHomes
    }
}
