package route_generator_new

class Travel (
    private val plan: List<TravelPoint>, currentTime: Double) {
    private val planLength = plan.size
    private var currentPosition : Int = 0
    private var isOnWay : Boolean = false
    private var isSkipLastPoint : Boolean = false
    private var homeId : String = plan[0].junctionId
    public var startTrialTime : Double = currentTime + plan[currentPosition].durationStop

    public fun startTrial(currentTime : Double) {
        if (startTrialTime > currentTime) {
            throw Exception("Duration of stop dont reached")
        }
        isOnWay = true
    }

    public fun stopTrial(currentTime : Double) {
        isSkipLastPoint = false
        isOnWay = false
        currentPosition++
        startTrialTime = currentTime + plan[currentPosition].durationStop
    }

    public fun skipPoint() {
        isSkipLastPoint = true
        isOnWay = false
        currentPosition++
    }

    public fun getIthPoint(i : Int) : TravelPoint{
        return plan[i]
    }

    public fun getPlanLength() : Int {
        return planLength
    }

    public fun getCurrentPosition() : Int {
        return currentPosition
    }

    public fun getHomeId() : String {
        return homeId
    }

    public fun getIsSkipPoint() : Boolean {
        return isSkipLastPoint
    }

    override fun toString(): String {
        val planString = plan.joinToString("\n") { it.toString() } // каждый TravelPoint в строку
        return "Travel(planLength=$planLength, currentPosition=$currentPosition, isOnWay=$isOnWay, isSkipLastPoint=$isSkipLastPoint, homeId='$homeId', \nplan=[$planString])"
    }


}
