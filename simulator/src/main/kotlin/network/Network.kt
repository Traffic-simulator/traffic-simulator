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

        // Connections of Lanes. Assign Lane objects as predecessors and successors for Lanes.
        for (road in roads) {
            // For Road <-> Road connections
            if (road.predecessor?.elementType == ERoadLinkElementType.ROAD) {
                // get all lanes that have predecessors
                road.lanes.filter { it.laneLink?.predecessor != null }.forEach {
                    // TODO get roads by id from hashmap
                    // for every lane find predecessor Lane object
                    lane -> lane.predecessor = roads.firstOrNull {
                        // get the predecessor road by id
                        predecessorRd -> predecessorRd.id == road.predecessor?.elementId
                    }?.lanes?.filter {
                        // filter lanes that are in laneLink by id
                        candidate -> lane.laneLink?.predecessor?.map {
                            prevLane -> prevLane.id
                        }?.contains(candidate.laneId.toBigInteger()) ?: false
                    }
                }

            // For Road <- Junction -> Road connections
            } else if (road.predecessor?.elementType == ERoadLinkElementType.JUNCTION) {
                val junc = junctions.firstOrNull { it.id == road.predecessor!!.elementId }!!  // that junction
                // get connections by incomingId, even for predecessor junction
                val connections = junc.connections[road.id]!!

                // if for our road Junction is a successor
                // then in laneLink[incomingRoad == ourRoad].from is -N

                for (lane in road.lanes) {
                    for (con in connections) {
                        // TODO can be many connected roads
                        // get incoming road, for predecessors AND successors incomingRoad is the key
                        val incomingRoad = roads.firstOrNull{ it.id == con.incomingRoad }!!

                        lane.predecessor = incomingRoad.lanes.filter {
                            con.laneLink.filter {
                                // find those laneLinks, that are connected FROM this lane
                                // even for predecessors
                                link -> link.from == lane.laneId.toBigInteger()
                            }.map {
                                // get TO lane indices from these laneLinks
                                link2 -> link2.to
                            }.contains(
                                // and make predicate to filter those incomingRoad.lanes
                                it.laneId.toBigInteger()
                            )
                        }
                    }
                }
            }

            // Road <-> Road
            if (road.successor?.elementType == ERoadLinkElementType.ROAD) {
                // same for successors
                road.lanes.filter { it.laneLink?.successor != null }.forEach {
                    // TODO get roads by id from hashmap
                    // for every lane find successor Lane object
                    lane -> lane.successor = roads.firstOrNull {
                        // get the successor road by id
                        successorRoads -> successorRoads.id == road.successor?.elementId
                    }?.lanes?.filter {
                        // filter lanes that are in laneLink by id
                        candidate -> lane.laneLink?.successor?.map {
                            nextLane -> nextLane.id
                        }?.contains(candidate.laneId.toBigInteger()) ?: false
                    }
                }
            // Road <- Junction -> Road
            } else if (road.successor?.elementType == ERoadLinkElementType.ROAD) {
                val junc = junctions.firstOrNull { it.id == road.successor!!.elementId }!!  // that junction
                // get connections by incomingId, even for predecessor junction
                val connections = junc.connections[road.id]!!

                // if for our road Junction is a successor
                // then in laneLink[incomingRoad == ourRoad].from is -N

                for (lane in road.lanes) {
                    for (con in connections) {
                        // TODO can be many connected roads
                        // get incoming road, for predecessors AND successors incomingRoad is the key
                        val incomingRoad = roads.firstOrNull { it.id == con.incomingRoad }!!

                        lane.successor = incomingRoad.lanes.filter {
                            con.laneLink.filter {
                                // find those laneLinks, that are connected FROM this lane
                                link -> link.from == lane.laneId.toBigInteger()
                            }.map {
                                // get TO lane indices from these laneLinks
                                link2 -> link2.to
                            }.contains(
                                // and make predicate to filter those incomingRoad.lanes
                                it.laneId.toBigInteger()
                            )
                        }
                    }
                }
            }
        }

        for (road in roads) {
            println("\nRoad" + road.id + ":")
            println("Road at junction?: " + (road.junction == "1"))
            println("Predecessor type id " + road.predecessor?.elementType + road.predecessor?.elementId)
            if (road.predecessor?.elementType == ERoadLinkElementType.JUNCTION) {
                for (con in junctions.firstOrNull{ it.id == road.predecessor?.elementId }!!.connections.values) {
                    println(con.map {
                            it -> "ROAD " + it.incomingRoad + " TO " + it.connectingRoad + ": "+
                                it.laneLink.map { "FROM " + it.from + " TO " + it.to }
                        }
                    )
                }
            }
            println("Successor type id " + road.successor?.elementType + road.successor?.elementId)
            if (road.successor?.elementType == ERoadLinkElementType.JUNCTION) {
                for (con in junctions.firstOrNull{ it.id == road.successor?.elementId }!!.connections.values) {
                    println(con.map {
                            it -> "ROAD " + it.incomingRoad + " TO " + it.connectingRoad + ": "+
                                it.laneLink.map { "FROM " + it.from + " TO " + it.to }
                        }
                    )
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
