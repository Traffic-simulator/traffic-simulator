package route_generator_new

import Waypoint
import route_generator.IRouteGenerator
import route_generator.RouteGeneratorDespawnListener
import route_generator.VehicleCreationListener
import route_generator.WaypointSpawnAbilityChecker
import route_generator_new.discrete_function.Building
import route_generator_new.discrete_function.TravelDesireFunction
import javax.swing.ListModel

class RouteGeneratorImpl(
    private val travelDesireFunction: TravelDesireFunction,
    private val startingTime: Double,     // In seconds
    private val buildings: List<Building>) : IRouteGenerator {

    private val model : Model = Model(travelDesireFunction, startingTime, buildings);
    private var despawnList = mutableListOf<Travel>()
    private var vehicleMap = HashMap<Int, Travel>()
    override fun update(
        dt: Double,
        create: VehicleCreationListener,
        isPositionFree: WaypointSpawnAbilityChecker
    ) {
        // TODO: use isPoistionFree please
        val spawnTravels = model.call(dt, despawnList)//получаем список travel'ов на спавн
        despawnList.clear()
        val spawnRoutes = transformTravelsToRoutes(spawnTravels)//переделываем его в Route'ы
        for (i in 0 until spawnRoutes.size) {
            val route = spawnRoutes[i]
            val startWaypoint = create.getWaypointByJunction(route.startJunctionId, true)
            val endWaypoint = create.getWaypointByJunction(route.endJunctionId, false)
            //создаем машинку по waypoint'ам
            val id = create.createVehicle(startWaypoint, endWaypoint, onDespawn)
            //creator возвращает Int?, не понимаю в каких случаях у нас может не заспавниться машинка
            if (id == null) {
                throw Exception("creator can't create a vehicle")
            }
            vehicleMap.put(id, spawnTravels[i])
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
                endTravelPoint = travel.getIthPoint(currentPosition)
                endJunctionId = endTravelPoint.junctionId
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
