package network.junction

import junction_intersection.Intersection
import mu.KotlinLogging
import network.Network
import opendrive.TJunction
import java.util.logging.Logger


// TODO: need more smart logic in case of different reasons blocks.
class Junction(val tjunction: TJunction, val intersections: MutableList<Intersection>, val network: Network) {

    private val logger = KotlinLogging.logger("BACKEND")

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
    }

    fun initTrajectories() {
        // Initializing TrajectoryBlockList
        for (con in tjunction.connection) {
            trajBlockList[con.connectingRoad] = TrajectoryBlockList(con, tjunction.connection, intersections, network)
            trajBlockingFactors[con.connectingRoad] = TrajectoryBlockingFactors()
        }
    }

    /**
     * Здесь есть проблема, она случается тогда, когда одна траектория заблочена чем-то,
     * А траектория с такой же входящей дорогой не заблочена.
     *
     * В этом случае сзади идущая машина может заблочить траекторию для впереди идущей, и все встанет.
     *
     * Есть два решения:
     *     1) Не блочить траектории с одной incoming road
     *     2) Игнорировать блокировки если они приходят от машин сзади на этой же дороге (стремно), но в нашей системе где нет двух подряд идущих дорог работать будет.
     *          Почти будет, так как мы можем заблочить следующий перекресток, находясь на предыдущем...
     */
    fun tryBlockTrajectoryVehicle(connectingRoadId: String, vehicleId: Int): Boolean {
        assert(trajBlockingFactors[connectingRoadId] != null)

        // TODO: Can not block if already blocked, perfomance optimization
        if (trajBlockingFactors[connectingRoadId]!!.blockingFactors.size != 0) {
            var blockingFactorsString: String = ""
            trajBlockingFactors[connectingRoadId]!!.blockingFactors.forEach {

                if (it.vehicleId == vehicleId) {
                    assert(it.vehicleId != vehicleId)
                }
                blockingFactorsString = blockingFactorsString.plus("Veh@" + it.vehicleId.toString() + " ")
            }

            blockingFactorsString = blockingFactorsString.trim()
            logger.debug("Veh@${vehicleId} desired trajectory with roadId@${connectingRoadId} is blocked by ${blockingFactorsString}, will stop before junction")
            return false
        }

        // Blocking trajectory
        trajBlockList[connectingRoadId]!!.blockList.forEach { trajBlockingFactors[it.connectingRoad]!!.addBlockingFactor(TrajectoryBlockingFactors.BlockingReason.DEFAULT, vehicleId) }
        logger.debug("Veh@${vehicleId} succesfully blocked trajectory with roadId@${connectingRoadId}")
        return true
    }

    fun unlockTrajectoryVehicle(connectingRoadId: String, vehicleId: Int) {
        assert(trajBlockingFactors[connectingRoadId] != null)

        trajBlockList[connectingRoadId]!!.blockList.forEach { trajBlockingFactors[it.connectingRoad]!!.removeBlockingFactor(TrajectoryBlockingFactors.BlockingReason.DEFAULT, vehicleId) }
        logger.debug("Veh@${vehicleId} unlocks trajectory with roadId@${connectingRoadId}")
    }

    // Not road specified option, will just check all possible trajectories
    fun unlockTrajectoryVehicle(vehicleId: Int) {
        trajBlockingFactors.forEach { connectingRoadId, factors ->
            factors.removeBlockingFactor(TrajectoryBlockingFactors.BlockingReason.DEFAULT, vehicleId)
        }
    }

}
