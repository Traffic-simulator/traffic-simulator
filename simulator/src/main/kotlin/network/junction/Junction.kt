package network.junction

import opendrive.TJunction


// TODO: need more smart logic in case of different reasons blocks.
class Junction(val tjunction: TJunction) {

    // HashMap by incoming roadId store connection
    val id: String = tjunction.id
    val connections: HashMap<String, ArrayList<Connection>> = HashMap()
//    val reversedConnections: HashMap<String, ArrayList<Connection>> = HashMap()

    // Connector id, BlockingList
    // Maybe generalize by using generic types?

    // Currently trajectory is a connector road
    // JunctionBlockingFactors - which lane will be blocked by

    // Trajectory block list - list of roads, that blocks if this trajectory became busy.
    // While we don't have geometry model all trajectories are blocked by all others (except self blocking)
    val trajBlockList = HashMap<String, TrajectoryBlockList>()


    // List of factors by which trajectory is blocked. Can be other vehicles, red light, and so on...
    val trajBlockingFactors = HashMap<String, TrajectoryBlockingFactors>()

    init {
        for (con in tjunction.connection) {
            if (con.incomingRoad in connections.keys) {
                connections[con.incomingRoad]?.add(Connection(con))
            } else {
                connections[con.incomingRoad] = ArrayList()
                connections[con.incomingRoad]?.add(Connection(con))
            }
        }

        // Initializing TrajectoryBlockList
        for (con in tjunction.connection) {
            trajBlockList[con.connectingRoad] = TrajectoryBlockList(con, tjunction.connection)
            trajBlockingFactors[con.connectingRoad] = TrajectoryBlockingFactors()
        }
    }

    fun tryBlockTrajectoryVehicle(connectingRoadId: String, vehicleId: Int): Boolean {
        assert(trajBlockingFactors[connectingRoadId] != null)
        if (trajBlockingFactors[connectingRoadId]!!.blockingFactors.size != 0) {
            return false
        }

        // Blocking trajectory
        trajBlockList.forEach { trajBlockingFactors[connectingRoadId]!!.addBlockingFactor(TrajectoryBlockingFactors.BlockingReason.DEFAULT, vehicleId) }
        return true
    }

    fun unlockTrajectoryVehicle(connectingRoadId: String, vehicleId: Int) {
        assert(trajBlockingFactors[connectingRoadId] != null)

        trajBlockList.forEach { trajBlockingFactors[connectingRoadId]!!.removeBlockingFactor(TrajectoryBlockingFactors.BlockingReason.DEFAULT, vehicleId) }
    }

}