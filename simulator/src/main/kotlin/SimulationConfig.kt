import route_generator_new.discrete_function.TravelDesireFunction

class SimulationConfig {

    // TODO: move to some kind of real config
    companion object {
        const val EPS = 0.0001
        const val INF = 1000000.0
        const val MIN_GAP = 2.0 // meters

        // Distance after which vehicle don't see other vehicles
        const val MAX_VALUABLE_DISTANCE = 300.0

        // Maybe have to be affected by speed
        const val LANE_CHANGE_DELAY = 5.0

        // Distance when vehicle can see other vehicles on junction, junction traffic lights
        // TODO: it would be good to depend on speed
        const val JUNCTION_BLOCK_DISTANCE = 50.0

        val defaultTravelDesireDistribution =
            TravelDesireFunction(listOf(
                10.0, 10.0, 10.0, 10.0, 10.0, 10.0, // 00:00 - 06:00
                10.0, 10.0, 10.0, 10.0, 10.0, 10.0, // 06:00 - 12:00
                10.0, 10.0, 10.0, 10.0, 10.0, 10.0, // 12:00 - 18:00
                10.0, 10.0, 10.0, 10.0, 10.0, 10.0  // 18:00 - 00:00
            ))
    }
}
