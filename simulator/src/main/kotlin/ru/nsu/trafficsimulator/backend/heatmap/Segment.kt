package ru.nsu.trafficsimulator.backend.heatmap

import ru.nsu.trafficsimulator.backend.network.Lane
import ru.nsu.trafficsimulator.backend.vehicle.Vehicle
import kotlin.math.exp

class Segment(lane: Lane, numFramesMemory: Int) {
    private val lastStates = SumTrackingArrayDeque(numFramesMemory)
    private val speedLimit = lane.getMaxSpeed()

    private var currentIterVehiclesSum: Double = 0.0
    private var currentIterVehiclesCount: Int = 0

    private var averageSpeed = speedLimit
    private var heatmapScore = 1.0

    fun getAverageSpeed() = averageSpeed
    fun getHeatmapScore() = heatmapScore

    fun update() {
        var meanSpeed = speedLimit
        if (currentIterVehiclesCount != 0) {
            meanSpeed = currentIterVehiclesSum / currentIterVehiclesCount
        }

        // чистим машинки для след апдейта
        currentIterVehiclesSum = 0.0
        currentIterVehiclesCount = 0

        lastStates.add(meanSpeed)
        averageSpeed = lastStates.average
        heatmapScore = customCenteredSigmoid(lastStates.average, speedLimit).coerceIn(0.0, 1.0)
    }

    fun addVehicleSpeed(vehicle: Vehicle) {
        currentIterVehiclesSum += vehicle.speed
        currentIterVehiclesCount += 1
    }

    fun clearMemory() {
        lastStates.clear()
    }

    private fun customCenteredSigmoid(
        t: Double,
        xEnd: Double = 1.0,
        steepness: Double = 20.0,
        midpointRatio: Double = 0.5
    ): Double {
        val midpoint = xEnd * midpointRatio
        val transformedT = steepness * (t - midpoint) / xEnd

        // Compute standard sigmoid
        val sig = 1.0 / (1.0 + exp(-transformedT))

        // Normalization factors
        val sig0 = 1.0 / (1.0 + exp(steepness * midpointRatio))
        val sigEnd = 1.0 / (1.0 + exp(-steepness * (1.0 - midpointRatio)))

        return (sig - sig0) / (sigEnd - sig0)
    }

    private class SumTrackingArrayDeque(private val limit: Int) {
        private val deque = ArrayDeque<Double>(limit + 1)
        private var currentSum: Double = 0.0

        val size: Int get() = deque.size
        val isEmpty: Boolean get() = deque.isEmpty()
        val average: Double get() = if (isEmpty) 0.0 else currentSum / size

        fun add(value: Double) {
            deque.addLast(value)
            currentSum += value
            enforceLimit()
        }

        fun clear() {
            deque.clear()
            currentSum = 0.0
        }

        private fun enforceLimit() {
            while (deque.size > limit) {
                val removed = deque.removeFirst()
                currentSum -= removed
            }
        }
    }
}
