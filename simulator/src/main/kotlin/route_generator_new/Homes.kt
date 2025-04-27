package route_generator_new

import route_generator_new.discrete_function.Building

class Homes {
    private val homes: HashMap<String, Building> = hashMapOf()
    private val numberOfAllHumans : Int
    private var numberOfHumansInHomes : Int
    private val emptyHomes: HashMap<String, Building> = hashMapOf()
    private val nonEmpty: HashMap<String, Building> = hashMapOf()
    private val fullHomes: HashMap<String, Building> = hashMapOf()

    constructor(buildings: List<Building>) {
        var currentPeople = 0;
        for(building in buildings) {
            if (building.type == BuildingTypes.HOME) {
                homes.put(building.junctionId, building)
                fullHomes.put(building.junctionId, building)
                nonEmpty.put(building.junctionId, building)

                currentPeople+=building.currentPeople;
            }
        }
        numberOfAllHumans = currentPeople;
        numberOfHumansInHomes = numberOfAllHumans
    }

    public fun getNumberOfHumansOnWay() : Int {
        return numberOfAllHumans - numberOfHumansInHomes
    }

    //метод добавляет человека к текущему зданию
    public fun increaseNumberOfPeopleInHome(junctionId: String) {
        val building : Building = homes[junctionId]!!
        //обновляем полные здания в случае заполнения
        if (building.currentPeople == building.capacity - 1) {
            fullHomes.put(building.junctionId, building)
        }
        //обновляем пустые здания в случае добавления туда человека
        if (building.currentPeople == 0) {
            emptyHomes.remove(building.junctionId)
            nonEmpty.put(building.junctionId, building)
        }
        building.currentPeople++
    }

    //метод удаляет человека из здания
    public fun decreaseNumberOfHumansInHome(junctionId: String) {
        val building : Building = homes[junctionId]!!
        //обновляем полные здания в случае удаления от туда человека
        if (building.currentPeople == building.capacity) {
            fullHomes.remove(building.junctionId)
        }
        //обновляем пустые здания в случае если здание опустело
        if (building.currentPeople == 1) {
            nonEmpty.remove(building.junctionId)
            emptyHomes.put(building.junctionId, building)
        }
        building.currentPeople++
    }

    public fun getNonEmpty() : HashMap<String, Building> = nonEmpty

    public fun decreaseNumberOfHumansInHomes(diff : Int) {
        numberOfHumansInHomes -= diff
        require(numberOfHumansInHomes >= 0)
    }

    public fun increaseNumberOfHumansInHomes(diff : Int) {
        numberOfHumansInHomes += diff
        require(numberOfHumansInHomes <= numberOfAllHumans)
    }

    public fun getNumberOfHumansInHomes() : Int {
        return numberOfHumansInHomes
    }




}
