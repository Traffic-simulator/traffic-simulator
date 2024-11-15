package ru.nsu.trafficsimulator

import network.Lane
import network.Road
import opendrive.OpenDRIVE
import opendrive.TRoad
import opendrive.TRoadLanesLaneSectionLrLane

interface Backend {
    /**
     * Class for communicating information about vehicles and their positions of the road
     * Distance specifies distance from the start of the road
     * Distance should be positive, and less than length of the road
     */
    data class Vehicle(val id: Int, val road: Road, val laneId: Int, val distance: Double)

    /**
     * Initialize simulation state with.
     * All information about points of interest, global settings, road rule settings
     * are embedded inside gAdditionalData list with type TUserData as specific elements
     * This method should not throw but return error as a return value
     * TODO: specify userData layout
     * @param layout Layout to initialize simulation
     */
    fun init(layout: OpenDRIVE): Error?

    /**
     * Calculate next vehicle positions
     */
    fun getNextFrame(): Array<Vehicle>
}
