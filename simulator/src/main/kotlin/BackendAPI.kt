import opendrive.OpenDRIVE
import vehicle.Direction
import vehicle.Vehicle

class BackendAPI : ISimulation{

    var simulator: Simulator? = null

    override fun init(layout: OpenDRIVE, spawnDetails: SpawnDetails, seed: Long): Error? {
        simulator = Simulator(layout, spawnDetails, seed)
        return null
    }

    override fun getNextFrame(deltaTime: Double): List<ISimulation.VehicleDTO> {
        if (simulator == null)
            return ArrayList<ISimulation.VehicleDTO>()

        val vehicles = simulator!!.update(deltaTime)

        return vehicles.map{ vehToDTO(it) }.toList()
    }

    fun vehToDTO(vehicle: Vehicle) : ISimulation.VehicleDTO {
        return ISimulation.VehicleDTO(
            vehicle.vehicleId,
            vehicle.lane.road.troad,
            vehicle.lane.laneId,
            ISimulation.VehicleType.PassengerCar,
            vehicle.position,
            Direction.FORWARD)
    }
}
