package ru.nsu.trafficsimulator.backend.route.route_generator_new

import ru.nsu.trafficsimulator.backend.route.route_generator_new.discrete_function.TravelDesireFunction

class ModelConfig {
    companion object {
        val defaultTravelDesireDistribution =
            TravelDesireFunction(listOf(
                2.0, 2.0, 2.0, 1.0, 1.0, 1.0, // 00:00 - 06:00
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0, // 06:00 - 12:00
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0, // 12:00 - 18:00
                1.0, 1.0, 1.0, 1.0, 1.0, 1.0  // 18:00 - 00:00
            ))

        val stayTimeMap = HashMap<BuildingTypes, Pair<Double, Double>>()
        init {
            //мапа содержит рамки времени in seconds, которое надо провести в здании определенного типа
            // ключ - тип | 1st - от | 2nd - до

            /*  Real-life version
                stayTimeMap[BuildingTypes.HOME] = Pair(0.0, 60 * 10.0)
                stayTimeMap[BuildingTypes.ENTERTAINMENT] = Pair(60 * 30.0, 60 * 120.0)
                stayTimeMap[BuildingTypes.EDUCATION] = Pair(60 * 90.0, 60 * 360.0)
                stayTimeMap[BuildingTypes.SHOPPING] = Pair(60 * 40.0, 60 * 120.0)
                stayTimeMap[BuildingTypes.WORK] = Pair(60 * 240.0, 60 * 480.0)
*/

            /*  Testing version   */
            stayTimeMap[BuildingTypes.HOME] = Pair(0.0, 10.0)
            stayTimeMap[BuildingTypes.ENTERTAINMENT] = Pair(30.0, 120.0)
            stayTimeMap[BuildingTypes.EDUCATION] = Pair(30.0, 120.0)
            stayTimeMap[BuildingTypes.SHOPPING] = Pair(30.0, 120.0)
            stayTimeMap[BuildingTypes.WORK] = Pair(30.0, 120.0)
        }
    }
}
