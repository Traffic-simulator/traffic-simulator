package vehicle

/**
 * Direction of lane is relation of road reference line to vehicles moving direction.
 * I.e. if direction is FORWARD then distance is computing from start of the reference line.
 * If direction is BACKWARD then distance is computing from end of the reference line.
 */
enum class Direction {
    FORWARD,
    BACKWARD;

    fun opposite(dir: Direction): Direction {
        if (dir == FORWARD) {
            return BACKWARD
        }
        return FORWARD
    }

    fun opposite(flag: Boolean): Direction {
        if (flag) {
            return this.opposite(this)
        }
        return this
    }
}
