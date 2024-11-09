package network

import opendrive.TJunction
import opendrive.TRoad

class Network(val troads: List<TRoad>, val tjunctions: List<TJunction>) {

    val roads: List<Road>
    val junctions: List<Junction>

    init {
        roads = troads.map{ Road(it) }
        junctions = tjunctions.map { Junction(it) }

        println("JUNCTIONS")
        for (junc in junctions) {
            println(junc)
            for (con in junc.connections) {
                println(con.id + ": " + con.incomingRoad + " to " + con.connectingRoad)
            }
        }

        println("ROADS")
        for (road in roads) {
            println(road.id)
        }
    }
}
