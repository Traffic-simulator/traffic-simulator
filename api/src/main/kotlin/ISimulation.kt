import signals.SignalState
import vehicle.Direction
import java.time.LocalTime

interface ISimulation {
    /**
     * All possible cars types that behave differently on the road
     * Each car type might have multiple variations as per supplied information at init time
     * 0th variation is default and guaranteed to exist
     */
    enum class VehicleType {
        PassengerCar,
        Bus;

        var variation: Int = 0
    }

    /**
     * Class for communicating information about vehicles and their positions of the road
     * Distance specifies distance from the start of the road
     * Distance should be positive, and less than length of the road
     */
    data class VehicleDTO(
        val id: Int,
        val road: opendrive.TRoad,
        val laneId: Int,
        val type: VehicleType,
        val distance: Double,
        val direction: Direction,
        val speed: Double,
        val source: String,
        val destination: String
    )

    /**
     * Class for info about signal states in the network.
     * distFromLaneStart - some kind of distance from the start of the lane
     * state - just a state
     * road/laneId - to specify that signal exactly we've got here
     */
    data class SignalDTO(val road: opendrive.TRoad, val laneId: Int, val distFromLaneStart: Double, val state: SignalState)

    /**
     * Class to create heatmap for lanes.
     */
    data class SegmentDTO(val road: opendrive.TRoad, val laneId: Int, val segmentLen: Double, val segments: List<Double>)

    /**
     * Initialize simulation state with.
     * All information about points of interest, global settings, road rule settings
     * are embedded inside gAdditionalData list with type TUserData as specific elements
     * This method should not throw but return error as a return value
     * TODO: specify userData layout
     * @param layout Layout to initialize simulation
     */
    fun init(layout: opendrive.OpenDRIVE,
             regionId: Int?,
             startingTime: LocalTime,
             seed: Long): Error?

    /**
     * Simulate simulation
     * @param deltaTime time interval to simulate in millis!
     */
    fun updateSimulation(deltaTimeMillis: Long)

    /**
     * Get states of vehicles in simulation
     */
    fun getVehicles(): List<VehicleDTO>

    /**
     * Getter for all signals in the network
     */
    fun getSignalStates(): List<SignalDTO>

    /**
     * Getter for all lane-segments DTOs. SegmentDTO assigned to TRoad+laneId.
     */
    fun getSegments(): List<SegmentDTO>

    /**
     * Get current time of simulation.
     */
    fun getSimulationTime(): LocalTime
}
