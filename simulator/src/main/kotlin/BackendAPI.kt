import mu.KotlinLogging
import network.Lane
import network.signals.Signal
import opendrive.OpenDRIVE
import route_generator_new.ModelConfig
import vehicle.Vehicle
import java.time.LocalTime

class BackendAPI : ISimulation {

    val logger = KotlinLogging.logger("BACKEND")
    var simulator: Simulator? = null

    override fun init(layout: OpenDRIVE, regionId: Int?, startingTime: LocalTime, seed: Long): Error? {
        simulator = Simulator(layout, startingTime, seed)
        return null
    }


    override fun updateSimulation(deltaTimeMillis: Long) {
        if (simulator == null)
            return

        val frametime = ISimulation.Constants.SIMULATION_FRAME_MILLIS
        assert(deltaTimeMillis % frametime == 0L)

        val iters = deltaTimeMillis / frametime
        val startNanos = System.nanoTime()
        for (i in 0 until iters) {
            simulator!!.update(frametime.toDouble() / 1000.0)
        }
        logger.info("Update took ${(System.nanoTime() - startNanos) / 1_000_000.0} milliseconds")
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

    private val SECONDS_IN_DAY = 60 * 60 * 24

    override fun getSimulationTime(): LocalTime {
        return LocalTime.ofSecondOfDay(simulator!!.currentTime.toLong() % SECONDS_IN_DAY)
    }

    override fun getIntersectionStats(): List<Pair<String, (id: Long) -> Any>> {
        return listOf(
            "Current people" to { id ->
                simulator?.buildings?.find { it.junctionId.toLong() == id }?.currentPeople ?: "Building not found"
            }
        )
    }

    override fun getVehicleStats(): List<Pair<String, (id: Int) -> Any>> {
        return listOf(
            "Current Speed" to { id ->
                simulator?.vehicles?.find { it.vehicleId == id }?.speed ?: "Vehicle not found"
            }
        )
    }

    fun vehToDTO(vehicle: Vehicle) : ISimulation.VehicleDTO {
        val lcInfo: ISimulation.LaneChangeDTO?
        if (!vehicle.isInLaneChange()) {
            lcInfo = null
        } else {
            lcInfo = ISimulation.LaneChangeDTO(
                vehicle.laneChangeFromLaneId,
                vehicle.lane.laneId,
                vehicle.laneChangeFullDistance,
                vehicle.laneChangeFullDistance - vehicle.laneChangeDistance
            )
        }

        return ISimulation.VehicleDTO(
            vehicle.vehicleId,
            vehicle.lane.road.troad,
            vehicle.lane.laneId,
            ISimulation.VehicleType.PassengerCar,
            vehicle.position,
            vehicle.direction,
            vehicle.speed,
            lcInfo,
            "Road id: ${vehicle.source.roadId}", // Maybe later will use not road ids
            "Road id: ${vehicle.destination.roadId}"
        )
    }

    fun signalToDTO(signal: Signal) : ISimulation.SignalDTO {
        return ISimulation.SignalDTO(
            signal.road,
            signal.laneId,
            signal.s,
            signal.state
        )
    }

    fun segmentToDTO(lane: Lane) : ISimulation.SegmentDTO {
        return ISimulation.SegmentDTO(
            lane.road.troad,
            lane.laneId,
            lane.lenOfSegment,
            lane.segments.map { it.getHeatmapScore() }
        )
    }
}
