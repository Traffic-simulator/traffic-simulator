package route_generator_new

import route_generator_new.discrete_function.Building
import route_generator_new.discrete_function.TravelDesireFunction
import kotlin.random.Random

class Model (
    private val travelDesireFunction: TravelDesireFunction,
    private val buildingsTypesWeight: HashMap<String, Double>,
    private val buildings: List<Building>) {

    companion object {
        private const val SECONDS_IN_HOUR = 3600;
        private const val HOURS_IN_DAY = 24;
        private const val maxPlanLength = 3
    }
    private val random = Random.Default
    private val homes : Homes;
    private var currentTime : Double
    private var meanOfTravelDesire: Double; //мат ожидание того сколько людей хотят поехать куда-нибудь
    private val buildingsMap = mutableMapOf<String, Building>()
    private val buildingsMapByType = mutableMapOf<BuildingTypes, MutableMap<String, Building>>()
    init {
        currentTime = 0.0
        meanOfTravelDesire = 0.0
        homes = Homes(buildings)

        for (type in BuildingTypes.entries) {
            buildingsMapByType.put(type, mutableMapOf())
        }

        for (building in buildings) {
            buildingsMap.put(building.junctionId, building)
            buildingsMapByType[building.type]!!.put(building.junctionId, building)
        }

    }

    fun call(deltaTime: Double) {
        addMeanOfTravelDesire(deltaTime)
    }

    //Функция добавляет значение к мат ожиданию
    fun addMeanOfTravelDesire(deltaTime: Double) {
        val numberOfPeopleInHome = homes.getNumberOfHumansInHomes()
        val currentHour = (currentTime.toLong() / SECONDS_IN_HOUR) % HOURS_IN_DAY
        val travelDesire = travelDesireFunction.getIthNumber(currentHour.toInt())
        meanOfTravelDesire += travelDesire / SECONDS_IN_HOUR * deltaTime * numberOfPeopleInHome;
        //TODO рассмотреть случай когда переходим из одного часа в другой, погрешность слишком мала, поэто не приоритетно
    }

    //создаем путь
    fun checkMeanOfTravelDesire() {
        while (meanOfTravelDesire >= 1.0) {
            var travel: Travel = createTravel();
        }
    }

    fun getRandomNonEmptyHome() : Building {
        var nonEmpty = homes.getNonEmpty();
        var numberOfNonEmptyHomes = nonEmpty.size;
        var indexOfHome = random.nextInt(numberOfNonEmptyHomes)
        var building = nonEmpty.toList().get(indexOfHome).second
        return building
    }


    fun createTravel() : Travel {
        val travelPoints = mutableListOf<TravelPoint>();
        var startHome = getRandomNonEmptyHome()
        var startPoint : TravelPoint = TravelPoint(startHome.junctionId, 0.0);
        var planLength = random.nextInt(maxPlanLength);
        travelPoints.add(startPoint);
        //init nonEmptyBuildingTypesList
        val listOfNonEmptyBuildingTypes = mutableListOf<BuildingTypes>()
        for (entry in buildingsMapByType) {
            if (entry.value.isNotEmpty()) {
                listOfNonEmptyBuildingTypes.add(entry.key)
            }
        }
        listOfNonEmptyBuildingTypes.remove(BuildingTypes.HOME)

        planLength = Math.min(planLength, listOfNonEmptyBuildingTypes.size);

        for (i in 0..planLength) {
            var type = listOfNonEmptyBuildingTypes.get(random.nextInt(listOfNonEmptyBuildingTypes.size));
            var buildingDictionaryByType = buildingsMapByType[type]!!;
            var building = buildingDictionaryByType.values.random();//TODO переделать через сидированный рандом
            var currentPoint = TravelPoint(building.junctionId, 20.0);//TODO переделать

            travelPoints.add(currentPoint);
        }

        travelPoints.add(startPoint);
        return Travel(travelPoints);
    }






}
