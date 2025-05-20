package route_generator_new

import route_generator_new.discrete_function.Building
import route_generator_new.discrete_function.TravelDesireFunction
import kotlin.random.Random

class Model (
    private val travelDesireFunction: TravelDesireFunction,
    private val buildings: List<Building>) {

    companion object {
        private const val SECONDS_IN_HOUR = 3600;
        private const val HOURS_IN_DAY = 24;
        private const val MAX_PLAN_LENGTH = 3
    }
    private val random = Random.Default
    private val homes : Homes;
    private var currentTime : Double
    private var meanOfTravelDesire: Double; //мат ожидание того сколько людей хотят поехать куда-нибудь
    private val buildingsMap = mutableMapOf<String, Building>()
    private val buildingsMapByType = mutableMapOf<BuildingTypes, MutableMap<String, Building>>()

    //приоритетеная очередь по delay
    private val travelQueue: TravelQueue = TravelQueue()


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

    /**
     * пути ассоциированы с машинками, у машинки есть путь
     *
     * deltaTime - время которое надо просимулировать с прошлого вызова
     * updatedTravels - список путей, которые доехали до некоторого контрольного пункта TravelPoint'а
     * (пока что предполагается что это когда машинка доехала до здания)
     * return MutableList<travel> возвращает список путей, которые находятся в состоянии что машинке нужно отъезжать от здания.
     * Это либо когда человек посидел в здании и направляется к следующему пункту,
     * либо если человек приехал, здание заполнено, он его пропускает и едет к следующему пункту
     */
    fun call(deltaTime: Double, updatedTravels: List<Travel>) : MutableList<Travel> {
        updateTravels(updatedTravels)
        //добавляем значение к мат. ожиданию, сколько людей должно поехать
        addMeanOfTravelDesire(deltaTime)
        currentTime += deltaTime
        checkMeanOfTravelDesire()
        val startTravelList = getNextTravels()
        return startTravelList
    }

    fun updateTravels(updatedTravels: List<Travel>) {
        for (travel in updatedTravels) {
            //доехали до дома
            if (travel.getPlanLength() == travel.getCurrentPosition() + 1) {
                homes.increaseNumberOfPeopleInHome(travel.getHomeId())
                continue
            }

            val junctionId = travel.getIthPoint(travel.getCurrentPosition() + 1).junctionId
            val building = buildingsMap[junctionId]
            if (building == null) {
                throw Exception("building not found")
            }

            //здание переполнено
            if (building.currentPeople == building.capacity) {
                //добавится в очередь с текущим временем, чтобы его сразу отправили к следующей точке
                //будто человек посмотрел что заполнено здание и развернулся
                travel.skipPoint()
            } else {
                // в здании есть места
                building.currentPeople++
                travel.stopTrial(currentTime)
            }

            travelQueue.addTravel(travel)
        }
    }

    fun getNextTravels() : MutableList<Travel> {
        val startTravelList = mutableListOf<Travel>()
        while (true) {
            val travel = travelQueue.peekNextTravel()
            if (travel != null && travel.startTrialTime <= currentTime) {
                travelQueue.getNextTravel()
                if (travel.getCurrentPosition() == 0) {
                    homes.decreaseNumberOfHumansInHome(travel.getHomeId())
                } else {
                    val junctionId = travel.getIthPoint(travel.getCurrentPosition()).junctionId
                    val building = buildingsMap[junctionId]
                    if (building == null) {
                        throw Exception("building not found")
                    }
                    if (!travel.getIsSkipPoint()) {
                        assert(building.currentPeople > 0)
                        building.currentPeople--
                    }
                }

                travel.startTrial(currentTime)
                startTravelList.add(travel)
                continue
            }
            break
        }
        return startTravelList
    }

    //Функция добавляет значение к мат ожиданию
    fun addMeanOfTravelDesire(deltaTime: Double) {
        val numberOfPeopleInHome = homes.getNumberOfHumansInHomes()
        val currentHour = (currentTime.toLong() / SECONDS_IN_HOUR) % HOURS_IN_DAY
        val travelDesire = travelDesireFunction.getIthNumber(currentHour.toInt())
        meanOfTravelDesire += travelDesire / SECONDS_IN_HOUR * deltaTime * numberOfPeopleInHome;
        //TODO рассмотреть случай когда переходим из одного часа в другой, погрешность слишком мала, поэтому не приоритетно
    }

    //создаем путь
    fun checkMeanOfTravelDesire() {
        while (meanOfTravelDesire >= 1.0) {
            var travel: Travel = createTravel()
            travelQueue.addTravel(travel)
            meanOfTravelDesire--
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
        var planLength = random.nextInt(MAX_PLAN_LENGTH);
        travelPoints.add(startPoint);
        //init nonEmptyBuildingTypesList
        //случай если здания определенного типа просто отсутствуют на карте
        //TODO вынести чтобы не повторялось при каждом вызове метода, здания все равно не добавятся в течении симуляции
        val listOfNonEmptyBuildingTypes = mutableListOf<BuildingTypes>()
        var numberOfNonHomesBuilding = 0
        for (entry in buildingsMapByType) {
            if (entry.value.isNotEmpty()) {
                //дома не интересны для генерации путей
                if (entry.key != BuildingTypes.HOME) {
                    listOfNonEmptyBuildingTypes.add(entry.key)
                    numberOfNonHomesBuilding += entry.value.size
                }
            }
        }

        planLength = Math.min(planLength, numberOfNonHomesBuilding);

        for (i in 0..planLength) {
            var type = listOfNonEmptyBuildingTypes.get(random.nextInt(listOfNonEmptyBuildingTypes.size));
            var buildingDictionaryByType = buildingsMapByType[type]!!;
            var building = buildingDictionaryByType.values.random();//TODO переделать через сидированный рандом
            var currentPoint = TravelPoint(building.junctionId, 60.0);//TODO переделать

            travelPoints.add(currentPoint);
        }

        travelPoints.add(startPoint);
        return Travel(travelPoints, currentTime);
    }
}
