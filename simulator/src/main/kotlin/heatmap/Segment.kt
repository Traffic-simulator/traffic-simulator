package heatmap

import SimulationConfig
import network.Lane
import vehicle.Vehicle
import kotlin.math.exp
import kotlin.math.tanh
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Segment(lane: Lane) {
    val lastStates = SumTrackingArrayDeque(5000)
    val speedLimit = lane.getMaxSpeed()
    var currentState: Double = 0.0
    var currentIterVehiclesSum: Double = 0.0
    var currentIterVehiclesCount: Int = 0

    fun update() {
        var meanSpeed = 1.0
        if (currentIterVehiclesCount != 0) {
            // [Simulation] For now vehicles can move faster than maxspeed :(
            meanSpeed = customCenteredSigmoid(currentIterVehiclesSum / currentIterVehiclesCount, speedLimit)
        }

        // чистим машинки для след апдейта
        currentIterVehiclesSum = 0.0
        currentIterVehiclesCount = 0

        // закидываем новое значение к последним и высчитываем среднее
        lastStates.add(meanSpeed)
        currentState = lastStates.average
    }

    fun customCenteredSigmoid(
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

    fun addVehicleSpeed(vehicle: Vehicle) {
        currentIterVehiclesSum += vehicle.speed
        currentIterVehiclesCount += 1
    }

    class SumTrackingArrayDeque(private val limit: Int) {
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

        private fun enforceLimit() {
            while (deque.size > limit) {
                val removed = deque.removeFirst()
                currentSum -= removed
            }
        }
    }
}
