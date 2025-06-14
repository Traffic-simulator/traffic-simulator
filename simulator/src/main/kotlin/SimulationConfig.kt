class SimulationConfig {

    // TODO: move to some kind of real config
    companion object {
        const val EPS = 0.0001
        const val INF = 100000000.0
        const val MIN_GAP = 2.0 // meters

        // Distance after which vehicle does not see other vehicles
        const val MAX_VALUABLE_DISTANCE = 300.0

        // Maybe have to be affected by speed
        const val LANE_CHANGE_DISTANCE_GAP = 20.0

        // Distance when vehicle can see other vehicles on junction, junction traffic lights
        // TODO: it would be good to depend on speed
        const val JUNCTION_BLOCK_DISTANCE = 50.0

        // Mandatory Lane Changes - MLC
        const val MLC_MIN_DISTANCE = 50.0

        // TODO: connect this value with frontend frametime
        const val SIMULATION_FRAME_MILLIS: Long = 20
    }
}
