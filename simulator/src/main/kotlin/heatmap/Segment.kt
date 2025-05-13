package heatmap

import vehicle.Vehicle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Segment {
    val lastStates: ArrayDeque<Double> by dequeLimiter(5)
    var currentState: Double = 0.0
    val currentIterVehicles: MutableList<Double> = mutableListOf()

    fun update() {
        val meanSpeed = currentIterVehicles.average()  // можно добавить сигмоиду чтобы было значение от 0 до 1
        // чистим машинки для след апдейта
        currentIterVehicles.clear()

        // закидываем новое значение к последним и высчитываем среднее
        lastStates.add(meanSpeed)
        currentState = lastStates.average()
    }

    fun addVehicleSpeed(vehicle: Vehicle) {
        currentIterVehicles.add(vehicle.speed)
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
