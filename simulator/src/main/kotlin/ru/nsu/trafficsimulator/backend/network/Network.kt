package ru.nsu.trafficsimulator.backend.network

import ISimulation
import ru.nsu.trafficsimulator.backend.junction_intersection.Intersection
import ru.nsu.trafficsimulator.backend.network.junction.Connection
import ru.nsu.trafficsimulator.backend.network.junction.Junction
import ru.nsu.trafficsimulator.backend.network.signals.Signal
import opendrive.*
import vehicle.Direction

// TODO: Interface for this class, because it's too big for reading
class Network(
    drivingSide: ISimulation.DrivingSide,
    troads: List<TRoad>,
    tjunctions: List<TJunction>,
    val intersections: MutableList<Intersection>,
    numFramesHeatmapMemory: Int) {

    val roads: List<Road> = troads.map{ Road(it, numFramesHeatmapMemory) }
    val roadById: HashMap<String, Road> = HashMap()

    val junctions: List<Junction> = tjunctions.map { Junction(it, intersections, this) }

    // Junctions that have Road in key as an incomingRoad
    val incidentJunctions: HashMap<Road, List<Junction>> = HashMap()

    fun getJunctionById(id: String): Junction {
        return junctions.filter { it.id == id }.get(0)
    }

    init {

        println("incidentRoads")
        for (road in roads) {
            incidentJunctions[road] = junctions.filter { road.id in it.connections.keys }
//            println(
//                road.id + "" + (
//                        // get incident Junctions (road is incomingRoad for them)
//                        incidentJunctions[road]?.map {
//                            // in every junction get connections from this road.id
//                            it.connections[road.id]?.map {
//                                // for every connection get the id of the road we connect to
//                                it2 -> it2.connectingRoad
//                            } ?: ""
//                        } ?: ""
//                        )
//            )
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
                    }?.map {
                        resLane -> if (road.predecessor?.contactPoint == null || road.predecessor?.contactPoint == EContactPoint.START) {
                            // We're connected to the start of the predecessor, change direction
                            Pair(resLane, true)
                        } else {
                            Pair(resLane, false)
                        }
                    } as ArrayList<Pair<Lane, Boolean>>?
                }

            // For Road <- Junction -> Road connections
            } else if (road.predecessor?.elementType == ERoadLinkElementType.JUNCTION) {
                val junc = junctions.firstOrNull { it.id == road.predecessor!!.elementId }!!  // that junction
                // get connections by incomingId, even for predecessor junction
                junc.connections[road.id]?.let { connections ->
                    for (lane in road.lanes) {
                        if (lane.predecessor == null) {
                            lane.predecessor = ArrayList()
                        }
                        for (con in connections) {
                            lane.predecessor!!.addAll(getLanesFromConnection(lane, con, true))
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
                    }?.map {
                        resLane -> if (road.successor?.contactPoint == null || road.successor?.contactPoint == EContactPoint.START) {
                            // We're connected to the start of the successor, don't change direction
                            Pair(resLane, false)
                        } else {
                            Pair(resLane, true)
                        }
                    } as ArrayList<Pair<Lane, Boolean>>?
                }
            // Road <- Junction -> Road
            } else if (road.successor?.elementType == ERoadLinkElementType.JUNCTION) {
                val junc = junctions.firstOrNull { it.id == road.successor!!.elementId }!!  // that junction
                // get connections by incomingId, even for successor junction
                junc.connections[road.id]?.let { connections ->
                    for (lane in road.lanes) {
                        if (lane.successor == null) {
                            lane.successor = ArrayList()
                        }
                        for (con in connections) {
                            lane.successor!!.addAll(getLanesFromConnection(lane, con, false))
                        }
                    }
                }
            }
        }

        roads.forEach { roadById.put(it.id, it) }
        junctions.forEach{ it.initTrajectories() }
        createLaneDirections(drivingSide)
    }

    private fun createLaneDirections(drivingSide: ISimulation.DrivingSide) {
        if (drivingSide == ISimulation.DrivingSide.LEFT) {
            throw RuntimeException("Left-side traffic currently unsupported")
        }

        // To detect lane direction we have to find roads that have only one connected side.
        // After that traverse all other lanes, using bfs...
        // After that check that all lanes are visited by as and there is no conflicts in directions.
        var directionAutoDetectionPossible = false
        val visited = HashSet<Lane>()
        val bfsQueue = ArrayDeque<Lane>()

        fun getLaneDirection(isSucc: Boolean, laneId: Int, drivingSide: ISimulation.DrivingSide): Direction {
            if (isSucc && laneId > 0 || !isSucc && laneId < 0) {
                when (drivingSide) {
                    ISimulation.DrivingSide.RIGHT -> return Direction.BACKWARD
                    ISimulation.DrivingSide.LEFT -> return Direction.FORWARD
                }
            } else {
                when (drivingSide) {
                    ISimulation.DrivingSide.RIGHT -> return Direction.FORWARD
                    ISimulation.DrivingSide.LEFT -> return Direction.BACKWARD
                }
            }
        }

        fun isRoadEnd(roadLink: TRoadLinkPredecessorSuccessor?): Boolean {
            if (roadLink == null) {
                return true
            }
            if (roadLink.elementType != ERoadLinkElementType.JUNCTION) {
                return false
            }
            return getJunctionById(roadLink.elementId).connections.size == 0
        }

        getAllLanes().forEach{
            lane ->
                if (isRoadEnd(lane.road.successor) || isRoadEnd(lane.road.predecessor)) {
                    directionAutoDetectionPossible = true
                    visited.add(lane)
                    bfsQueue.add(lane)

                    lane.direction = getLaneDirection(isRoadEnd(lane.road.successor), lane.laneId, drivingSide)
                }
        }

        if (!directionAutoDetectionPossible) {
            throw Exception("Can not detect directions of the road, add one-side-connected road.")
        }

        while(!bfsQueue.isEmpty()) {
            val top = bfsQueue.removeFirstOrNull()!!

            top.successor?.forEach{
                succ ->
                    if (!visited.contains(succ.first)) {
                        visited.add(succ.first)
                        bfsQueue.add(succ.first)
                        succ.first.direction = top.direction!!.opposite(succ.second)
                    }
            }

            top.predecessor?.forEach{
                pred ->
                    if (!visited.contains(pred.first)) {
                        visited.add(pred.first)
                        bfsQueue.add(pred.first)
                        pred.first.direction = top.direction!!.opposite(pred.second)
                    }
            }
        }

        val exc = getAllLanes().firstOrNull { it.direction == null }
        if (exc != null) {
            throw Exception("Unreachable lane! RoadId: ${exc.road.id}, laneId: ${exc.laneId}.")
        }
    }

    fun getRoadById(id: String): Road {
        return roadById[id]!!
    }

    fun getLaneById(roadId: String, laneId: String): Lane {
        return getLaneById(roadId, laneId.toInt())
    }

    fun getLaneById(roadId: String, laneId: Int): Lane {
        val road = getRoadById(roadId)
        return road.getLaneById(laneId)
    }

    fun getAllLanes(): List<Lane> {
        val lanes = ArrayList<Lane>()
        roads.forEach { lanes.addAll(it.lanes) }

        return lanes
    }

    fun getLanesFromConnection(
        currentLane: Lane,
        con: Connection,
        processingPredecessor: Boolean
    ): List<Pair<Lane, Boolean>> {
        // Junction: [Connection[laneLink]]

        //  \/ our lane is at this road      \/ and we want a connected lane from this road
        // incomingRoad <- Connection -> connectingRoad

        // get the road we're connected through Junction
        val connectingRoad = roads.firstOrNull { it.id == con.connectingRoad }!!
        val resLaneList = connectingRoad.lanes.filter {
            // we're processing
            con.laneLink.filter {
                // find those laneLinks, that are connected TO this lane
                link -> link.from == currentLane.laneId.toBigInteger()
            }.map {
                // get FROM lane indices from these laneLinks
                link2 -> link2.to
            }.contains(
                // and make predicate to filter those incomingRoad.lanes
                it.laneId.toBigInteger()
            )
        }

        if (con.contactPoint == null || con.contactPoint == EContactPoint.START) {
            return resLaneList.map {
                resLane -> if (processingPredecessor) {
                    // Road is a predecessor and contactPoint is START, so changing direction
                    Pair(resLane, true)
                } else {
                    Pair(resLane, false)
                }
            }
        } else {
            return resLaneList.map {
                resLane -> if (processingPredecessor) {
                    // Road is a predecessor and contactPoint is END, so not changing direction
                    Pair(resLane, false)
                } else {
                    Pair(resLane, true)
                }
            }
        }
    }

    fun verbose() {
        for (road in roads) {
            println("\nRoad ${road.id}:")
            println("Road at junction?: ${road.junction != "-1"}")
            println("Predecessor type id ${road.predecessor?.elementType}${road.predecessor?.elementId} " +
                "contactPoint ${road.predecessor?.contactPoint}")
            if (road.predecessor?.elementType == ERoadLinkElementType.JUNCTION) {
                for (con in junctions.firstOrNull{ it.id == road.predecessor?.elementId }!!.connections.values) {
                    println(con.map {
                            it -> "ROAD ${it.incomingRoad} TO ${it.connectingRoad}: "+
                            it.laneLink.map { "FROM ${it.from} TO ${it.to}" }
                    }
                    )
                }
            }
            println("Successor type id ${road.successor?.elementType}${road.successor?.elementId} " +
                "contactPoint ${road.successor?.contactPoint}")
            if (road.successor?.elementType == ERoadLinkElementType.JUNCTION) {
                for (con in junctions.firstOrNull{ it.id == road.successor?.elementId }!!.connections.values) {
                    println(con.map {
                            it -> "ROAD ${it.incomingRoad} TO ${it.connectingRoad}: "+
                            it.laneLink.map { "FROM ${it.from} TO ${it.to}" }
                    }
                    )
                }
            }
            println(
                road.lanes.map {
                    "\n predecessorLane" + it.predecessor?.map {
                        it2 -> it2.first.roadId + " : "  + it2.first.laneId + " changeDir?" + it2.second
                    } + "->" + "currentLane" + it.laneId
                }
            )
            println(
                road.lanes.map {
                    "\n currentLane" + it.laneId  + "->" + "successorLane" +
                            it.successor?.map {
                                it2 -> it2.first.roadId + " : "  + it2.first.laneId + " changeDir?" + it2.second
                            }
                }
            )

            println("Signals:")
            println(
                road.lanes.map {
                    "\n currentLane ${it.laneId} signal: cycle=${it.signal?.cycle}"
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

    fun updateSignals(deltaTime: Double) {
        for (road in roads) {
            for (lane in road.lanes) {
                if (lane.signal != null) {
                    lane.signal!!.updateState(deltaTime)
                }
            }
        }
    }

    fun getSignals() : List<Signal> {
        val signals: ArrayList<Signal> = ArrayList()

        for (road in roads) {
            for (lane in road.lanes) {
                if (lane.signal != null) {
                    signals.add(lane.signal!!)
                }
            }
        }

        return signals
    }
}
