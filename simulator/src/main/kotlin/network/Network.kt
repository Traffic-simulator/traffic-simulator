package network

import opendrive.TRoad

class Network(val troads: List<TRoad>) {

    val roads: List<Road>

    init {
        roads = troads.map{it -> Road(it)}
    }
}