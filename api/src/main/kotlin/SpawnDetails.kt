import vehicle.Direction
import java.util.ArrayList

/**
 * This class will be removed when a better spawn model is available.
 *
 * First element of pair - RoadId.
 * Second element of pair - LaneId.
 */
class SpawnDetails(val spawnPair: ArrayList<Triple<String, String, Direction>>) {
}
