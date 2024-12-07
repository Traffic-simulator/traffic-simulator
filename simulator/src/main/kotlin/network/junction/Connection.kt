package network.junction

import opendrive.TJunctionConnection

class Connection(tConnection: TJunctionConnection) {
    val connectingRoad = tConnection.connectingRoad
    val incomingRoad = tConnection.incomingRoad
    val linkedRoad = tConnection.linkedRoad // Probably deprecated or not used
    val id = tConnection.id
    val laneLink = tConnection.laneLink
    val contactPoint = tConnection.contactPoint
}
