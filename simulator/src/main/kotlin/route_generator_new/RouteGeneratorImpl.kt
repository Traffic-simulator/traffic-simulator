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
    private val buildings: List<Building>) : IRouteGenerator {

    private val model : Model = Model(travelDesireFunction, buildings);
    private var despawnList = mutableListOf<Travel>()
    private var vehicleMap = HashMap<Int, Travel>()
    override fun update(
        dt: Double,
        create: VehicleCreationListener,
        isPositionFree: WaypointSpawnAbilityChecker
    ) {
        val spawnTravels = model.call(dt, despawnList)//получаем список travel'ов на спавн
        despawnList.clear()
        val spawnRoutes = transformTravelsToRoutes(spawnTravels)//переделываем его в Route'ы
        for (i in 0 until spawnRoutes.size) {
            val route = spawnRoutes[i]
            val waypoints = getWaypointsByRoute(route)//TODO матвей получит по junctionId wayopoint'ы
            val startWaypoint = waypoints.first
            val endWaypoint = waypoints.second
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


    private fun getWaypointsByRoute(route: Route): Pair<Waypoint, Waypoint> {
        val waypoints : Pair<Waypoint, Waypoint>
        TODO("Add waypoints to \"waypoints\" first - startWaypoint, second - endWaypoint")
    }

    //функция достает из Travel старт от куда спавнить машинку и конец куда она поедет
    private fun transformTravelsToRoutes(travels: List<Travel>) : List<Route> {
        val routes = mutableListOf<Route>()
        for (travel in travels) {
            val currentPosition = travel.getCurrentPosition()

            val startTravelPoint = travel.getIthPoint(currentPosition)
            val startJunctionId = startTravelPoint.junctionId

            val endTravelPoint = travel.getIthPoint(currentPosition + 1)
            val endJunctionId = endTravelPoint.junctionId

            val route = Route(startJunctionId, endJunctionId)
            routes.add(route)
        }

        return routes
    }


}
