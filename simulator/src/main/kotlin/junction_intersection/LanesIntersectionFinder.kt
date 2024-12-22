package junction_intersection

interface LanesIntersectionFinder {
    fun findIntersections(lanes1: Lanes, lanes2: Lanes) : List<Intersection>
}
