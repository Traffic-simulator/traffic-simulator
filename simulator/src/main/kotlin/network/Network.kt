package network

import opendrive.ERoadLinkElementType
import opendrive.TJunction
import opendrive.TRoad

class Network(val troads: List<TRoad>, val tjunctions: List<TJunction>) {

    // DONE store predecessors and successors id for roads
    // DONE store predecessor and successor for lanes
    val roads: List<Road> = troads.map{ Road(it) }
    val junctions: List<Junction> = tjunctions.map { Junction(it) }

    // Junctions that have Road in key as an incomingRoad
    val incidentJunctions: HashMap<Road, List<Junction>> = HashMap()

    init {

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

        // Assign Lane objects as predecessors and successors to Lanes
        // If junction they'll stay null
        for (road in roads) {
            println("\nRoad" + road.id + ":")
            println("Predecessor type id " + road.predecessor?.elementType + road.predecessor?.elementId)

            if (road.predecessor?.elementType == ERoadLinkElementType.ROAD) {
                // get all lanes that have predecessors
                road.lanes.takeWhile { it.laneLink.predecessor != null }.forEach {
                    // TODO get roads by id from hashmap
                    // for every lane find predecessor Lane object
                    lane -> lane.predecessor = roads.firstOrNull {
                        // get the predecessor road by id
                        predecessorRd -> predecessorRd.id == road.predecessor?.elementId
                    }?.lanes?.filter {
                        // filter lanes that are in laneLink by id
                        candidate -> lane.laneLink.predecessor.map {
                            prevLane -> prevLane.id
                        }.contains(candidate.laneId.toBigInteger())
                    }
                }
            }

            println("Successor type id " + road.successor?.elementType + road.successor?.elementId)
            if (road.successor?.elementType == ERoadLinkElementType.ROAD) {
                // same for successors
                road.lanes.takeWhile { it.laneLink.successor != null }.forEach {
                    // TODO get roads by id from hashmap
                    // for every lane find successor Lane object
                    lane -> lane.successor = roads.firstOrNull {
                        // get the successor road by id
                        successorRoads -> successorRoads.id == road.successor?.elementId
                    }?.lanes?.filter {
                        // filter lanes that are in laneLink by id
                        candidate -> lane.laneLink.successor.map {
                            nextLane -> nextLane.id
                        }.contains(candidate.laneId.toBigInteger())
                    }
                }
            }

            println(
                road.lanes.map {
                    "\n predecessorLane" + it.predecessor?.map { it2 -> it2.laneId } + "->" + "currentLane" + it.laneId
                }
            )
            println(
                road.lanes.map {
                    "\n currentLane" + it.laneId  + "->" + "successorLane" + it.successor?.map { it2 -> it2.laneId }
                }
            )
        }
    }

    fun getNextLane(currentRoad: Road, currentLane: Lane, nextRoad: Road) {
        val nextJunction =
            incidentJunctions[currentRoad]!![0]  // let's assume that we have only 1 junction at the end of the road
        val nextConnection = nextJunction.connections.filter { it.key == nextRoad.id }  // connection to the next road
        // TODO get the lane from connection or from the lane
        return
    }
}
