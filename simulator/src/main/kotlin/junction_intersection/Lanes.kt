package junction_intersection

class Lanes (
    val referenceLine: Spline,
    val leftRoadsList: List<Int>,
    val rightRoadsList: List<Int>
) {
    //TODO add some lists of left lines points
    companion object {
        val NUMBER_OF_SECTIONS : Int = 100
    }
    private val positiveLanes : MutableList<Lane> = mutableListOf()
    private val negativeLanes : MutableList<Lane> = mutableListOf()
    init {
        for (id in leftRoadsList) {
            positiveLanes.add(Lane(referenceLine, id))
        }
        for (id in rightRoadsList) {
            negativeLanes.add(Lane(referenceLine, id))
        }
    }



}
