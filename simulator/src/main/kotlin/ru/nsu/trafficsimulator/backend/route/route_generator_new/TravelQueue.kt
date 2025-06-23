package ru.nsu.trafficsimulator.backend.route.route_generator_new

import java.util.*

class TravelQueue {
    private val queue: PriorityQueue<Travel> = PriorityQueue { t1, t2 ->
        t1.startTrialTime.compareTo(t2.startTrialTime)
    }

    fun addTravel(travel: Travel) {
        queue.add(travel)
    }

    fun getNextTravel(): Travel? {
        return queue.poll() // Извлекает и удаляет элемент с наименьшим delay
    }

    fun peekNextTravel(): Travel? {
        return queue.peek() // Возвращает элемент с наименьшим delay, не удаляя его
    }

    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }
}
