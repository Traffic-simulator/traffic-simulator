package heatmap

import vehicle.Vehicle
import kotlin.math.tanh
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Segment {
    val lastStates: ArrayDeque<Double> by dequeLimiter(5000)
    var currentState: Double = 0.0
    var currentIterVehiclesSum: Double = 0.0
    var currentIterVehiclesCount: Int = 0

    fun update() {
        var meanSpeed = 1.0
        if (currentIterVehiclesCount != 0) {
            meanSpeed = tanh(currentIterVehiclesSum / currentIterVehiclesCount)
        }

        // чистим машинки для след апдейта
        currentIterVehiclesSum = 0.0
        currentIterVehiclesCount = 0

        // закидываем новое значение к последним и высчитываем среднее
        lastStates.add(meanSpeed)
        currentState = lastStates.average()
    }

    fun addVehicleSpeed(vehicle: Vehicle) {
        currentIterVehiclesSum += vehicle.speed
        currentIterVehiclesCount += 1
    }

    fun <E> dequeLimiter(limit: Int): ReadWriteProperty<Any?, ArrayDeque<E>> =
        object : ReadWriteProperty<Any?, ArrayDeque<E>> {

            private var deque: ArrayDeque<E> = ArrayDeque(limit)

            private fun applyLimit() {
                while (deque.size > limit) {
                    val removed = deque.removeFirst()
                    println("dequeLimiter removed $removed")
                }
            }

            override fun getValue(thisRef: Any?, property: KProperty<*>): ArrayDeque<E> {
                applyLimit()
                return deque
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: ArrayDeque<E>) {
                this.deque = value
                applyLimit()
            }
        }
}
