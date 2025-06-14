import mu.KotlinLogging
import network.Lane
import heatmap.Segment
import network.signals.Signal
import opendrive.OpenDRIVE
import route_generator_new.BuildingTypes
import route_generator_new.discrete_function.Building
import vehicle.Vehicle

class BackendAPI : ISimulation {

    val logger = KotlinLogging.logger("BACKEND")
    var simulator: Simulator? = null

    override fun init(layout: OpenDRIVE, seed: Long): Error? {
        val buildingParser = BuildingsParser(layout)
        val buildings = buildingParser.getBuildings()

        simulator = Simulator(layout, buildings, seed)
        return null
    }

    override fun updateSimulation(deltaTimeMillis: Long) {
        if (simulator == null)
            return

        // TODO: Probably SimulationConfig should not be used here
        assert(deltaTimeMillis % SimulationConfig.SIMULATION_FRAME_MILLIS == 0L)

        val iters = deltaTimeMillis / SimulationConfig.SIMULATION_FRAME_MILLIS
        val startNanos = System.nanoTime()
        for (i in 0 until iters) {
            simulator!!.update(SimulationConfig.SIMULATION_FRAME_MILLIS.toDouble() / 1000.0)
        }
        logger.info("Update took ${(System.nanoTime() - startNanos) / 1000000.0} milliseconds")
    }

    override fun getVehicles(): List<ISimulation.VehicleDTO> {
        if (simulator == null)
            return ArrayList<ISimulation.VehicleDTO>()

        return simulator!!.vehicles.map{ vehToDTO(it) }.toList()
    }

    override fun getSignalStates(): List<ISimulation.SignalDTO> {
        if (simulator == null)
            return ArrayList<ISimulation.SignalDTO>()

        val signals = simulator!!.network.getSignals()

        return signals.map{ signalToDTO(it)}.toList()
    }

    override fun getSegments(): List<ISimulation.SegmentDTO> {
        if (simulator == null)
            return ArrayList<ISimulation.SegmentDTO>()

        val lanes = simulator!!.network.getAllLanes()

        return lanes.map { segmentToDTO(it) }.toList()
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

    fun segmentToDTO(lane: Lane) : ISimulation.SegmentDTO {
        return ISimulation.SegmentDTO(
            lane.road.troad,
            lane.laneId,
            lane.lenOfSegment,
            lane.segments.map { it.currentState }
        )
    }
}
