package ru.nsu.trafficsimulator.backend.utils

class TimedCache<K, V>(
    private val secondsTimeout: Double,
) {
    private val cache = HashMap<K, CacheEntry<V>>()

    private data class CacheEntry<V>(
        val value: V,
        val expirationTime: Double
    )

    fun get(key: K, currentTime: Double, compute: (K) -> V): V {
        val entry = cache[key]
        if (entry != null) {
            if (currentTime < entry.expirationTime) {
                return entry.value
            } else {
                cache[key] = CacheEntry(compute(key), currentTime + secondsTimeout)
            }
        }
        cache[key] = CacheEntry(compute(key), currentTime + secondsTimeout)
        return cache[key]!!.value
    }

}
