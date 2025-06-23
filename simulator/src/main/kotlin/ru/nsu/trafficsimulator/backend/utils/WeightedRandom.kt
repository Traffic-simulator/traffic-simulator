package ru.nsu.trafficsimulator.backend.utils

import kotlin.random.Random

class WeightedRandom(seed: Long) {
    private val random = Random(seed)

    /**
     * Selects a random index from the list where each element's probability
     * is its value divided by the sum of all values
     *
     * @param weights List of non-negative weights
     * @return Index of the selected element
     * @throws IllegalArgumentException if weights are empty, negative, or all zero
     */
    fun chooseIndex(weights: List<Int>): Int {
        require(weights.isNotEmpty()) { "Weights list cannot be empty" }

        val total = weights.sum().toDouble()
        require(weights.all { it >= 0 }) { "Weights cannot be negative" }
        require(total > 0) { "At least one weight must be positive" }

        val randomValue = random.nextDouble() * total
        var accumulated = 0.0

        for ((index, weight) in weights.withIndex()) {
            accumulated += weight
            if (randomValue < accumulated) {
                return index
            }
        }

        // Fallback (should theoretically never reach here if math is correct)
        return weights.lastIndex
    }
}

