package network

import opendrive.TJunction
import opendrive.TJunctionConnection

class Junction(val tjunction: TJunction) {

    // HashMap by incoming roadId store connection
    val connections: HashMap<String, ArrayList<Connection>> = HashMap()

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
}
