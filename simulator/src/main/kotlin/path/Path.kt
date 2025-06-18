package path

import network.Lane

class Path {

    enum class PWType {
        MLC,
        NORMAL
    }

    data class PathWaypoint(
        val type: PWType,
        val lane: Lane,
        val mlcMaxRoadOffset: Double
    )

}
