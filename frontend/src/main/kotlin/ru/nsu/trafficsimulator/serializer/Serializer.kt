package ru.nsu.trafficsimulator.serializer

import opendrive.*
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class Serializer {
    fun serialize(layout: Layout): OpenDRIVE {
        val openDrive = OpenDRIVE()

        return openDrive
    }


    private fun serializeRoad(road: Road): TRoad {
        return TRoad().apply {
            id = road.id.toString()
            length = road.len
            junction = "-1"

            link.predecessor = TRoadLinkPredecessorSuccessor().apply {
                elementType = ERoadLinkElementType.JUNCTION
                elementId = road.startIntersection.id.toString()
            }
            link.successor = TRoadLinkPredecessorSuccessor().apply {
                elementType = ERoadLinkElementType.JUNCTION
                elementId = road.endIntersection.id.toString()
            }

            planView = TRoadPlanView().apply {
                // TODO
                geometry
            }

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
                        leftLane.type = ELaneType.DRIVING
                        leftLane.level = TBool.FALSE

                        val successor = TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor()
                        successor.id = i.toBigInteger()
                        leftLane.link.successor.add(successor)
                        val predecessor = TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor()
                        successor.id = i.toBigInteger()
                        leftLane.link.predecessor.add(predecessor)

                        left.lane.add(leftLane)
                    }

                    center = TRoadLanesLaneSectionCenter()
                    val centerLane = TRoadLanesLaneSectionCenterLane()
                    centerLane.id = 0.toBigInteger()
                    centerLane.type = ELaneType.NONE
                    centerLane.level = TBool.FALSE
                    center.lane.add(centerLane)

                    right = TRoadLanesLaneSectionRight()
                    for (i in 1..road.rightLane) {
                        val rightLane = TRoadLanesLaneSectionRightLane()
                        rightLane.id = (-i).toBigInteger()
                        rightLane.type = ELaneType.DRIVING
                        rightLane.level = TBool.FALSE

                        val successor = TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor()
                        successor.id = (-i).toBigInteger()
                        rightLane.link.successor.add(successor)
                        val predecessor = TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor()
                        successor.id = (-i).toBigInteger()
                        rightLane.link.predecessor.add(predecessor)

                        right.lane.add(rightLane)
                    }
                })
            }
        }
    }
}
