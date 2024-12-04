package ru.nsu.trafficsimulator.serializer

import OpenDriveWriter
import opendrive.*
import ru.nsu.trafficsimulator.model.*

class Serializer {
    fun serialize(layout: Layout): OpenDRIVE {
        val openDrive = OpenDRIVE()

        for (road in layout.layoutRoads) {
            openDrive.road.add(serializeRoad(road))
        }

        for (intersectionRoad in layout.layoutIntersectionRoads) {
            openDrive.road.add(serializeIntersectionRoad(intersectionRoad))
        }

        for (intersection in layout.layoutIntersections) {
            openDrive.junction.add(serializeIntersection(intersection))
        }
        return openDrive
    }


    private fun serializeRoad(road: Road): TRoad {
        return TRoad().apply {
            id = road.id.toString()
            length = road.length
            junction = "-1" // Без соединений с перекрёстками

            // Тип дороги
            type.add(TRoadType().apply {
                s = 0.0
                type = ERoadType.TOWN
                country = "DE"
            })

            // План дороги (геометрия)
            planView = TRoadPlanView().apply {
                geometry.add(TRoadPlanViewGeometry().apply {
                    s = 0.0
                    x = 120.0
                    y = 137.0
                    hdg = Math.PI / 2 // 90 градусов в радианах
                    length = road.length
                    line = TRoadPlanViewGeometryLine() // Прямая линия
                })
            }

            // Профиль дороги
            elevationProfile = TRoadElevationProfile().apply {
                elevation.add(TRoadElevationProfileElevation().apply {
                    s = 0.0
                    a = 0.0
                    b = 0.0
                    c = 0.0
                    d = 0.0
                })
            }

            // Полосы
            lanes = TRoadLanes().apply {
                laneSection.add(TRoadLanesLaneSection().apply {
                    s = 0.0

                    left = TRoadLanesLaneSectionLeft().apply {
                        for (i in 1..road.leftLane) {
                            lane.add(TRoadLanesLaneSectionLeftLane().apply {
                                id = i.toBigInteger()
                                type = ELaneType.DRIVING
                                level = TBool.FALSE
                                borderOrWidth.add(TRoadLanesLaneSectionLrLaneWidth().apply {
                                    sOffset = 0.0
                                    a = 3.75
                                    b = 0.0
                                    c = 0.0
                                    d = 0.0
                                })
                            })
                        }
                    }

                    center = TRoadLanesLaneSectionCenter().apply {
                        lane.add(TRoadLanesLaneSectionCenterLane().apply {
                            id = 0.toBigInteger()
                            type = ELaneType.DRIVING
                            level = TBool.FALSE
                            roadMark.add(TRoadLanesLaneSectionLcrLaneRoadMark().apply {
                                sOffset = 0.0
                                type = ERoadMarkType.BROKEN
                                color = ERoadMarkColor.STANDARD
                                width = 0.12
                            })
                        })
                    }

                    right = TRoadLanesLaneSectionRight().apply {
                        for (i in 1..road.rightLane) {
                            lane.add(TRoadLanesLaneSectionRightLane().apply {
                                id = (-i).toBigInteger()
                                type = ELaneType.DRIVING
                                level = TBool.FALSE
                                borderOrWidth.add(TRoadLanesLaneSectionLrLaneWidth().apply {
                                    sOffset = 0.0
                                    a = 3.75
                                    b = 0.0
                                    c = 0.0
                                    d = 0.0
                                })
                            })
                        }
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

    odr.write(od, "testOpenDrive.xodr")


}
