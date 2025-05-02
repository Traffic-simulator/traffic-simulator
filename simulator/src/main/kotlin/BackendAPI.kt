import mu.KotlinLogging
import opendrive.OpenDRIVE
import vehicle.Direction
import vehicle.Vehicle

class BackendAPI : ISimulation{

    val logger = KotlinLogging.logger("BACKEND")
    var simulator: Simulator? = null

    override fun init(layout: OpenDRIVE, spawnDetails: ArrayList<Waypoint>, despawnDetails: ArrayList<Waypoint>, seed: Long): Error? {
        simulator = Simulator(layout, spawnDetails, despawnDetails, seed)
        return null
    }

    override fun getNextFrame(deltaTime: Double): List<ISimulation.VehicleDTO> {
        if (simulator == null)
            return ArrayList<ISimulation.VehicleDTO>()

        val startNanos = System.nanoTime()

        val vehicles = simulator!!.update(deltaTime)
        val result = vehicles.map{ vehToDTO(it) }.toList()

        logger.info("Update took ${(System.nanoTime() - startNanos) / 1000000.0} milliseconds")
        return result
    }

    fun vehToDTO(vehicle: Vehicle) : ISimulation.VehicleDTO {
        return ISimulation.VehicleDTO(
            vehicle.vehicleId,
            vehicle.lane.road.troad,
            vehicle.lane.laneId,
            ISimulation.VehicleType.PassengerCar,
            vehicle.position,
            vehicle.direction)
    }
}
