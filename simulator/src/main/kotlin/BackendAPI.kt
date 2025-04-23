import network.signals.Signal
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

    override fun getSignalStates(deltaTime: Double): List<ISimulation.SignalDTO> {
        if (simulator == null)
            return ArrayList<ISimulation.SignalDTO>()

        val signals = simulator!!.network.getSignals(deltaTime)

        return signals.map{ signalToDTO(it)}.toList()
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

    fun signalToDTO(signal: Signal) : ISimulation.SignalDTO {
        return ISimulation.SignalDTO(
            signal.road,
            signal.laneId,
            signal.t,
            signal.state
        )
    }
}
