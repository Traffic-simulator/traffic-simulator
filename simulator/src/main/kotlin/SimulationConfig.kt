class SimulationConfig {

    // TODO: move to some kind of real config
    companion object {
        const val EPS = 0.0001
        const val INF = 1000000.0

        // Distance after which vehicle don't see other vehicles
        const val MAX_VALUABLE_DISTANCE = 300.0

        // Maybe have to be affected by speed
        const val LANE_CHANGE_DELAY = 5.0

        // Distance when vehicle can see other vehicles on junction, junction traffic lights
        // TODO: it would be good to depend on speed
        const val JUNCTION_BLOCK_DISTANCE = 80.0
    }
}
