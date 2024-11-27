package ru.nsu.trafficsimulator.serializer

import OpenDriveWriter
import opendrive.*
import ru.nsu.trafficsimulator.model.*

class Serializer {
    fun serialize(layout: Layout): OpenDRIVE {
        val openDrive = OpenDRIVE()

        for (road in layout.roads) {
            openDrive.road.add(serializeRoad(road))
        }

        for (intersectionRoad in layout.intersectionRoads) {
            openDrive.road.add(serializeIntersectionRoad(intersectionRoad))
        }

        for (intersection in layout.intersections) {
            openDrive.junction.add(serializeIntersection(intersection))
        }
        return openDrive
    }


    private fun serializeRoad(road: Road): TRoad {
        return TRoad().apply {
            id = road.id.toString()
            length = road.length
            junction = "-1"

            link = TRoadLink()
            link.predecessor = TRoadLinkPredecessorSuccessor().apply {
                elementType = ERoadLinkElementType.JUNCTION
                elementId = road.startIntersection.id.toString()
            }
            link.successor = TRoadLinkPredecessorSuccessor().apply {
                elementType = ERoadLinkElementType.JUNCTION
                elementId = road.endIntersection.id.toString()
            }

            planView = generateRoadPlaneView(road)

            lanes = TRoadLanes().apply {
                laneOffset.add(TRoadLanesLaneOffset().apply {
                    a = 0.0
                    b = 0.0
                    c = 0.0
                    d = 0.0
                    s = 0.0
                })

                laneSection.add(TRoadLanesLaneSection().apply {
                    s = 0.0

                    left = TRoadLanesLaneSectionLeft()
                    for (i in 1..road.leftLane) {
                        val leftLane = TRoadLanesLaneSectionLeftLane()
                        leftLane.id = i.toBigInteger()
                        left.lane.add(leftLane)
                    }

                    center = TRoadLanesLaneSectionCenter()
                    val centerLane = TRoadLanesLaneSectionCenterLane()
                    centerLane.id = 0.toBigInteger()
                    center.lane.add(centerLane)

                    right = TRoadLanesLaneSectionRight()
                    for (i in 1..road.rightLane) {
                        val rightLane = TRoadLanesLaneSectionRightLane()
                        rightLane.id = (-i).toBigInteger()
                        right.lane.add(rightLane)
                    }
                })
            }
        }
    }

    private fun serializeIntersectionRoad(road: IntersectionRoad): TRoad {
        return TRoad().apply {
            id = road.id.toString()
            length = road.length
            junction = road.intersection.id.toString()

//            link = TRoadLink()
//            link.predecessor = TRoadLinkPredecessorSuccessor().apply {
//                elementType = ERoadLinkElementType.JUNCTION
//                elementId = road.startIntersection.id.toString()
//            }
//            link.successor = TRoadLinkPredecessorSuccessor().apply {
//                elementType = ERoadLinkElementType.JUNCTION
//                elementId = road.endIntersection.id.toString()
//            }

//            planView = generateRoadPlaneView(road)

            lanes = TRoadLanes().apply {
                laneOffset.add(TRoadLanesLaneOffset().apply {
                    a = 0.0
                    b = 0.0
                    c = 0.0
                    d = 0.0
                    s = 0.0
                })

                laneSection.add(TRoadLanesLaneSection().apply {
                    s = 0.0

//                    left = TRoadLanesLaneSectionLeft()
//                    for (i in 1..road.leftLane) {
//                        val leftLane = TRoadLanesLaneSectionLeftLane()
//                        leftLane.id = i.toBigInteger()
//                        left.lane.add(leftLane)
//                    }
//
//                    center = TRoadLanesLaneSectionCenter()
//                    val centerLane = TRoadLanesLaneSectionCenterLane()
//                    centerLane.id = 0.toBigInteger()
//                    center.lane.add(centerLane)
//
//                    right = TRoadLanesLaneSectionRight()
//                    for (i in 1..road.rightLane) {
//                        val rightLane = TRoadLanesLaneSectionRightLane()
//                        rightLane.id = (-i).toBigInteger()
//                        right.lane.add(rightLane)
//                    }
                })
            }
        }
    }

    private fun generateRoadPlaneView(road: Road): TRoadPlanView {
        return TRoadPlanView().apply {
            // TODO
        }
    }

    private fun serializeIntersection(intersection: Intersection): TJunction {
        return TJunction().apply {
            id = intersection.id.toString()
        }
    }
}

fun main() {
    val odr = OpenDriveWriter()
    val ser = Serializer()

    val layout = Layout()
    layout.addRoad(Point(1.0, 1.0, 1.0), Point(2.0, 2.0, 2.0))

    val od = ser.serialize(layout)

    odr.write(od, "testOpenDrive")


}
