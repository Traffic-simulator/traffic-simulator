package ru.nsu.trafficsimulator.backend.path

import ru.nsu.trafficsimulator.backend.network.Lane

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
