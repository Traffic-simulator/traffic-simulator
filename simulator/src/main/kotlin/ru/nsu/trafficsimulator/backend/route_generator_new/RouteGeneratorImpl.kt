package ru.nsu.trafficsimulator.backend.route_generator_new

import ru.nsu.trafficsimulator.backend.network.Waypoint
import ru.nsu.trafficsimulator.backend.route_generator.IRouteGenerator
import ru.nsu.trafficsimulator.backend.route_generator.RouteGeneratorDespawnListener
import ru.nsu.trafficsimulator.backend.route_generator.VehicleCreationListener
import ru.nsu.trafficsimulator.backend.route_generator.WaypointSpawnAbilityChecker
import ru.nsu.trafficsimulator.backend.route_generator_new.discrete_function.Building

class RouteGeneratorImpl(
    startingTime: Double,     // In seconds
    buildings: List<Building>,
    seed: Long) : IRouteGenerator {

    private val model : Model = Model(startingTime, buildings, seed)

    private var despawnList = mutableListOf<Travel>()
    private var vehicleMap = HashMap<Int, Travel>()

    private var needToSpawnRoutes = mutableListOf<Triple<Waypoint, Waypoint, Travel>>() // 1st - start | 2nd - end | 3rd - travel
    override fun update(
        dt: Double,
        create: VehicleCreationListener,
        isPositionFree: WaypointSpawnAbilityChecker
    ) {
        val needToSpawnRoutesVar = mutableListOf<Triple<Waypoint, Waypoint, Travel>>()
        //создаем машинки, которые должны были создать в прошлые разы, но линии были заняты
        for (i in 0 until needToSpawnRoutes.size) {
            val route = needToSpawnRoutes[i]
            if (isPositionFree.isFree(route.first)) {
                val id = create.createVehicle(route.first, route.second, onDespawn)
                if (id == null) {
                    throw Exception("creator can't create a vehicle")
                }
                vehicleMap[id] = route.third
            } else {
                //если не получилось, то перекидываем на следующие разы
                needToSpawnRoutesVar.add(route)
            }
        }
        needToSpawnRoutes = needToSpawnRoutesVar

        val spawnTravels = model.call(dt, despawnList)//получаем список travel'ов на спавн
        despawnList.clear()
        val spawnRoutes = transformTravelsToRoutes(spawnTravels)//переделываем его в Route'ы
        for (i in 0 until spawnRoutes.size) {
            val route = spawnRoutes[i]
            if (route.endJunctionId == "-100") {
                continue
            }
            val startWaypoint = create.getWaypointByJunction(route.startJunctionId, true)
            val endWaypoint = create.getWaypointByJunction(route.endJunctionId, false)
            //создаем машинку по waypoint'ам
            val travel = spawnTravels[i]
            if (isPositionFree.isFree(startWaypoint)) {
                val id = create.createVehicle(startWaypoint, endWaypoint, onDespawn)
                //creator возвращает Int?, не понимаю в каких случаях у нас может не заспавниться машинка
                if (id == null) {
                    throw Exception("creator can't create a vehicle")
                }
                vehicleMap[id] = travel
            } else {
                needToSpawnRoutes.add(Triple(startWaypoint, endWaypoint, travel))
            }
        }

    }

    private val onDespawn = object : RouteGeneratorDespawnListener {
        override fun onDespawn(vehicleId: Int) {
            val travel = vehicleMap[vehicleId]
            if (travel == null) {
                throw Exception("VehicleMap don't contain travel with vehicleId: $vehicleId")
            }
            despawnList.add(travel)
            vehicleMap.remove(vehicleId)
        }
    }

    //функция достает из Travel старт от куда спавнить машинку и конец куда она поедет
    private fun transformTravelsToRoutes(travels: List<Travel>) : List<Route> {
        val routes = mutableListOf<Route>()
        for (travel in travels) {
            val currentPosition = travel.getCurrentPosition()

            val startTravelPoint = travel.getIthPoint(currentPosition)
            val startJunctionId = startTravelPoint.junctionId

            var endTravelPoint: TravelPoint
            var endJunctionId: String
            if (currentPosition + 1 == travel.getPlanLength()) {
                endJunctionId = "-100"
            } else {
                endTravelPoint = travel.getIthPoint(currentPosition + 1)
                endJunctionId = endTravelPoint.junctionId
            }


            val route = Route(startJunctionId, endJunctionId)
            routes.add(route)
        }

        return routes
    }


}
