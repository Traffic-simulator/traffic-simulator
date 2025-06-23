package ru.nsu.trafficsimulator.backend.junction_intersection

class Lanes (
    val roadId: String,
    val referenceLine: Spline,
    val leftRoadsList: List<Long>,
    val rightRoadsList: List<Long>
) {
    //TODO add some lists of left lines points
    val positiveLanes : MutableList<Lane> = mutableListOf()
    val negativeLanes : MutableList<Lane> = mutableListOf()
    init {
        for (id in leftRoadsList) {
            positiveLanes.add(Lane(referenceLine, id))
        }
        for (id in rightRoadsList) {
            negativeLanes.add(Lane(referenceLine, id))
        }
    }



}
