package network.junction

class TrajectoryBlockingFactors {

    // TODO: Architecure detail, How to store different params depending on reason?
    // VehicleId - костыль
    class BlockingFactor(val reason: BlockingReason, val vehicleId: Int) {

    }

    // Can store level of blocking, time limit and so on ...
    // Currently just some MagicNumbers
    enum class BlockingReason(val priority: Int) {
        TRAFFIC_LIGHT(100),
        SECONDARY_ROAD(50),
        PRIORITY_TO_RIGHT(30),
        DEFAULT(1),
    }

    var blockingFactors = ArrayList<BlockingFactor>()

    // Be careful. Can be muptiple same adds.
    fun addBlockingFactor(reason: BlockingReason, vehicleId: Int) {
        blockingFactors.add(BlockingFactor(reason, vehicleId))
    }

    fun removeBlockingFactor(reason: BlockingReason, vehicleId: Int) {
        // TODO: need smarter logic
        blockingFactors = blockingFactors.filter{ it.reason != reason || it.vehicleId != vehicleId }.toList() as ArrayList<BlockingFactor>
    }

//    fun update(deltaTime: double) {
//
//    }

}
