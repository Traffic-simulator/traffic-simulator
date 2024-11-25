package vehicle

/**
 * Direction of vehicle in relation of road reference line direction.
 * I.e. if direction is FORWARD then distance is computing from start of the reference line.
 * If direction is BACKWARD then distance is computing from end of the reference line.
 *
 * Maybe will be excluded later...
 */
enum class Direction {
    FORWARD,
    BACKWARD;
}
