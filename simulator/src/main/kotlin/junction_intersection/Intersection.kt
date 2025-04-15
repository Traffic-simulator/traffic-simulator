package junction_intersection

class Intersection (
    var roadId1: String,
    var laneId1: Long,
    var roadId2: String,
    var laneId2: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Intersection

        return roadId1 == other.roadId1 &&
            laneId1 == other.laneId1 &&
            roadId2 == other.roadId2 &&
            laneId2 == other.laneId2
    }
}
