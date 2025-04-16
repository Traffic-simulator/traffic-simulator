package route_generator

import Waypoint
import java.util.*
import kotlin.collections.ArrayList

class RouteGeneratorAPI(val rnd: Random, val spawnDetails: ArrayList<Waypoint>, val despawnDetails: ArrayList<Waypoint>) {
    val timer = 2.0
    var spawnTimer = timer

    // TODO: connect with buildings logic. API will stay the same. Probably have to add some despawn callback...
    fun update(dt: Double, create: (Waypoint, Waypoint) -> Boolean, isPositionFree: (Waypoint) -> Boolean) {
        spawnTimer += dt
        if (spawnTimer < timer) return

        val availablePositions = spawnDetails.filter { isPositionFree(it) }
        if (availablePositions.isEmpty()) return
        val fromIdx = rnd.nextInt(availablePositions.size)
        val toIdx = rnd.nextInt(despawnDetails.size)
        create(availablePositions[fromIdx], despawnDetails[toIdx])

        spawnTimer = 0.0
    }

}
