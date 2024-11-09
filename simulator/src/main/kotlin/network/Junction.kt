package network

import opendrive.TJunction
import opendrive.TJunctionConnection

class Junction(val tjunction: TJunction) {
    val connections: List<TJunctionConnection>

    init {
        connections = tjunction.connection
    }
}