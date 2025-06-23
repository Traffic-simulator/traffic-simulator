package ru.nsu.trafficsimulator.backend.network

data class LaneSequence(
    val lane: Lane,
    val acc_distance: Double,
    val initial_iteration: Boolean
)
