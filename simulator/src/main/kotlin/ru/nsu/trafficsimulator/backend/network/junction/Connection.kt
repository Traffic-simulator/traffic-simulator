package ru.nsu.trafficsimulator.backend.network.junction

import opendrive.EContactPoint
import opendrive.TJunctionConnection

class Connection(tConnection: TJunctionConnection) {
    val connectingRoad = tConnection.connectingRoad
    val incomingRoad = tConnection.incomingRoad
    val linkedRoad = tConnection.linkedRoad // Probably deprecated or not used
    val id = tConnection.id
    val laneLink = tConnection.laneLink
    val contactPoint = if (tConnection.contactPoint == null) EContactPoint.START else tConnection.contactPoint
}
