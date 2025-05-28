package route_generator_new

class TravelPoint (
    val junctionId: String,//место положение куда поехать
    val durationStop: Double//длительность остановки там
){
    override fun toString(): String {
        return "TravelPoint(junctionId='$junctionId', durationStop='$durationStop')"
    }
}
