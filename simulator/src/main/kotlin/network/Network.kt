package network

import opendrive.TJunction
import opendrive.TRoad

class Network(val troads: List<TRoad>, val tjunctions: List<TJunction>) {

    val roads: List<Road>
    val junctions: List<Junction>

    // Junctions that have Road in key as an incomingRoad
    val incidentJunctions: HashMap<Road, List<Junction>> = HashMap()

    init {
        roads = troads.map{ Road(it) }
        junctions = tjunctions.map { Junction(it) }

//        println("JUNCTIONS")
//        for (junc in junctions) {
//            println(junc)
//            for (con in junc.connections) {
//                println(con.key + " to " + con.value.map { it.connectingRoad })
//            }
//        }

        println("incidentRoads")
        for (road in roads) {
            incidentJunctions[road] = junctions.filter { road.id in it.connections.keys }
            println(
                road.id + "" + (
                        // get incident Junctions (road is incomingRoad for them)
                        incidentJunctions[road]?.map {
                            // in every junction get connections from this road.id
                            it.connections[road.id]?.map {
                                // for every connection get the id of the road we connect to
                                it2 -> it2.connectingRoad
                            } ?: ""
                        } ?: ""
                        )
            )
        }
    }

    fun getNextLane(currentRoad: Road, currentLane: Lane, nextRoad: Road) {
        val nextJunction =
            incidentJunctions[currentRoad]!![0]  // let's assume that we have only 1 junction at the end of the road
        val nextConnection = nextJunction.connections.filter { it.key == nextRoad.id }  // connection to the next road
        // TODO get the lane from connection or from the nextConnection.connectingRoad
        return
    }
}
