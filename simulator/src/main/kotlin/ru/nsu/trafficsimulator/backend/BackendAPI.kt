package ru.nsu.trafficsimulator.backend

import ISimulation
import mu.KotlinLogging
import ru.nsu.trafficsimulator.backend.network.Lane
import ru.nsu.trafficsimulator.backend.network.signals.Signal
import opendrive.OpenDRIVE
import opendrive.TRoad
import opendrive.TUserData
import ru.nsu.trafficsimulator.backend.vehicle.Vehicle
import java.time.Duration
import java.time.LocalTime

class BackendAPI : ISimulation {

    val logger = KotlinLogging.logger("BACKEND")
    var simulator: Simulator? = null

    override fun init(layout: OpenDRIVE, drivingSide: ISimulation.DrivingSide, regionId: Int?, startingTime: LocalTime, seed: Long): Error? {
        simulator = Simulator(layout,  drivingSide, startingTime, seed)
        return null
    }

    override fun gatherSimulationStats(layout: OpenDRIVE, seed: Long): OpenDRIVE {
        logger.info { "Starting gathering stats "}
        val startTime = LocalTime.now()
        val framerate: Long = 50      // in milliseconds
        val logPeriod: Long = 20 * 60 // in seconds (20 min)
        val SECONDS_IN_DAY: Long = 24 * 60 * 60
        val numFramesHeatmapMemory = (logPeriod * 1000 / framerate).toInt() // Have to store heatmap values for each logging step

        val sim = Simulator(layout, ISimulation.DrivingSide.RIGHT, LocalTime.of(0, 0, 0), seed, numFramesHeatmapMemory)

            // roadId, <negativeSideAvgSpeed, positiveSideAvgSpeed>
        val roadSideFlowSpeed = HashMap<String, ArrayList<Pair<Double, Double>>>()

        // TODO: спрашивать время у симулятора...
        for (currentTime in 0 until SECONDS_IN_DAY * 1000 step framerate) {
            sim.update(framerate.toDouble() / 1000.0)
            if (currentTime % (logPeriod * 1000) == 0L) {

                // getRoadLaneSideFlowSpeed
                for(road in sim.network.roads) {
                    if (!roadSideFlowSpeed.containsKey(road.id)) {
                        roadSideFlowSpeed[road.id] = ArrayList<Pair<Double, Double>>()
                    }
                    roadSideFlowSpeed[road.id]!!.add(Pair(road.negativeSideAvgSpeed, road.positiveSideAvgSpeed))
                }
                sim.clearHeatmapData()

                val percent = currentTime.toDouble() / (SECONDS_IN_DAY * 10)
                logger.info { "${"%.3f".format(percent) }% of stats gathering passed."}
            }
        }

        // store stats in layout
        layout.getRoad().forEach {
            if (!roadSideFlowSpeed.containsKey(it.id)) {
                throw RuntimeException("Backend stats gathering exception")
            }
            it.addUserData("avgFlowLogPeriodSeconds", logPeriod.toString())
            val negativeSideList = roadSideFlowSpeed.get(it.id)!!.map { it.first }
            val positiveSideList = roadSideFlowSpeed.get(it.id)!!.map { it.second }

            it.addUserData("negativeSideAvgFlowSpeed", serializeList(negativeSideList))
            it.addUserData("positiveSideAvgFlowSpeed", serializeList(positiveSideList))
        }

        val finishTime = LocalTime.now()
        logger.info { "Stats gathering successfully finished in ${Duration.between(startTime, finishTime).seconds} seconds. "}
        return layout
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
            vehicle.lane.direction!!,
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

fun createUserData(key: String, value: String) = TUserData().apply {
    this.code = key
    this.value = value
}

private fun serializeList(list: List<Double>) = list.joinToString(" ") { "%.3f".format(it) }

private fun TRoad.addUserData(key: String, value: String) =
    this.getGAdditionalData().add(createUserData(key, value))


