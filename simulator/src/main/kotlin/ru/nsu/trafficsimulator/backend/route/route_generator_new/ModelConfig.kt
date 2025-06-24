package ru.nsu.trafficsimulator.backend.route.route_generator_new

import ru.nsu.trafficsimulator.backend.route.route_generator_new.discrete_function.TravelDesireFunction

class ModelConfig {
    companion object {
        val defaultTravelDesireDistribution =
            TravelDesireFunction(listOf(
                0.005, 0.005, 0.005, 0.005, 0.005, 0.01, // 00:00 - 06:00
                0.1, 0.17, 0.2 /*8am - 9am*/, 0.15, 0.1, 0.1, // 06:00 - 12:00
                0.05, 0.02, 0.02, 0.01, 0.01, 0.01, // 12:00 - 18:00
                0.01, 0.01, 0.01, 0.005, 0.005, 0.005  // 18:00 - 00:00
            ))

        val stayTimeMap = HashMap<BuildingTypes, Pair<Double, Double>>()
        init {
            //мапа содержит рамки времени in seconds, которое надо провести в здании определенного типа
            // ключ - тип | 1st - от | 2nd - до

            /*  Real-life version */
            stayTimeMap[BuildingTypes.HOME] = Pair(60 * 60.0, 60 * 10.0)
            stayTimeMap[BuildingTypes.ENTERTAINMENT] = Pair(60 * 30.0, 60 * 120.0)
            stayTimeMap[BuildingTypes.EDUCATION] = Pair(60 * 300.0, 60 * 360.0)
            stayTimeMap[BuildingTypes.SHOPPING] = Pair(60 * 40.0, 60 * 120.0)
            stayTimeMap[BuildingTypes.WORK] = Pair(60 * 450.0, 60 * 520.0)


            /*  Testing version   */
/*
            stayTimeMap[BuildingTypes.HOME] = Pair(0.0, 10.0)
            stayTimeMap[BuildingTypes.ENTERTAINMENT] = Pair(30.0, 120.0)
            stayTimeMap[BuildingTypes.EDUCATION] = Pair(30.0, 120.0)
            stayTimeMap[BuildingTypes.SHOPPING] = Pair(30.0, 120.0)
            stayTimeMap[BuildingTypes.WORK] = Pair(30.0, 120.0) */
        }
    }
}
