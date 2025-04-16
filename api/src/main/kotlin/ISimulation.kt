import vehicle.Direction

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
    data class VehicleDTO(val id: Int, val road: opendrive.TRoad, val laneId: Int, val type: VehicleType, val distance: Double, val direction: Direction)

    /**
     * Initialize simulation state with.
     * All information about points of interest, global settings, road rule settings
     * are embedded inside gAdditionalData list with type TUserData as specific elements
     * This method should not throw but return error as a return value
     * TODO: specify userData layout
     * @param layout Layout to initialize simulation
     */
    fun init(layout: opendrive.OpenDRIVE, spawnDetails: ArrayList<Waypoint>, despawnDetails: ArrayList<Waypoint>, seed: Long): Error?

    /**
     * Calculate next vehicle positions after a set amount of time
     * @param deltaTime time interval to simulate
     */
    fun getNextFrame(deltaTime: Double): List<VehicleDTO>
}
