package route_generator_new

import route_generator_new.discrete_function.Building

class Homes {
    private val homes: MutableList<Building> = mutableListOf()
    private val numberOfAllHumans : Int
    private var numberOfHumansInHomes : Int

    constructor(buildings: List<Building>) {
        var currentPeople = 0;
        for(building in buildings) {
            if (building.type == BuildingTypes.HOME) {
                homes.add(building)
                currentPeople+=building.currentPeople;
            }
        }
        numberOfAllHumans = currentPeople;
        numberOfHumansInHomes = numberOfAllHumans
    }

    public fun getNumberOfHumansOnWay() : Int {
        return numberOfAllHumans - numberOfHumansInHomes
    }

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
