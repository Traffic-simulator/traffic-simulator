package route_generator_new

import route_generator_new.discrete_function.Building
import route_generator_new.discrete_function.TravelDesireFunction

class Model (
    private val travelDesireFunction: TravelDesireFunction,
    private val buildingsTypesWeight: HashMap<String, Double>,
    private val buildings: List<Building>) {

    companion object {
        private const val SECONDS_IN_HOUR = 3600;
        private const val HOURS_IN_DAY = 24;
    }

    private val homes : Homes;
    private var currentTime : Double
    private var meanOfTravelDesire: Double; //мат ожидание того сколько людей хотят поехать куда-нибудь
    init {
        currentTime = 0.0
        meanOfTravelDesire = 0.0
        homes = Homes(buildings)
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

    }

    fun createPaths() {

    }




}
