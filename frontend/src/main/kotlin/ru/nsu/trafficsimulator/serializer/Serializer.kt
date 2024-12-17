package ru.nsu.trafficsimulator.serializer

import opendrive.*
import ru.nsu.trafficsimulator.model.*
import kotlin.math.cos
import kotlin.math.sin

fun serializeLayout(layout: Layout): OpenDRIVE {
    val openDrive = OpenDRIVE()

    for (road in layout.roads.values) {
        openDrive.road.add(serializeRoad(road))
    }

    for (intersectionRoad in layout.intersectionRoads.values) {
        openDrive.road.add(serializeIntersectionRoad(intersectionRoad))
    }

    for (intersection in layout.intersections.values) {
        openDrive.junction.add(serializeIntersection(intersection))
    }
    return openDrive
}


private fun serializeRoad(road: Road): TRoad {
    return TRoad().apply {
        id = road.id.toString()
        length = road.geometry.length
        junction = "-1"

        link = TRoadLink()
        road.startIntersection?.let {
            link.predecessor = TRoadLinkPredecessorSuccessor()
            link.predecessor.elementType = ERoadLinkElementType.JUNCTION
            link.predecessor.elementId = it.id.toString()
        }

        road.endIntersection?.let {
            link.successor = TRoadLinkPredecessorSuccessor()
            link.successor.elementType = ERoadLinkElementType.JUNCTION
            link.successor.elementId = it.id.toString()
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
        length = road.geometry.length
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
    val tRoadPlanViewGeometry = TRoadPlanView()

    for (roadGeometry in road.geometry.splineParts) {
        val (start, direction) = roadGeometry.getStartPoint()
        val rotAngle = -((direction - start).angle())
        val x: Poly3
        val y: Poly3
        roadGeometry.let {
            x = Poly3(it.x.a - start.x, it.x.b, it.x.c, it.x.d)
            y = Poly3(it.y.a - start.y, it.y.b, it.y.c, it.y.d)
        }
        val rotatedX = x * cos(rotAngle) - y * sin(rotAngle)
        val rotatedY = x * sin(rotAngle) + y * cos(rotAngle)

        val paramPoly3 = TRoadPlanViewGeometryParamPoly3().apply {
            au = rotatedX.a
            bu = rotatedX.b
            cu = rotatedX.c
            du = rotatedX.d
            av = rotatedY.a
            bv = rotatedY.b
            cv = rotatedY.c
            dv = rotatedY.d
        }

        tRoadPlanViewGeometry.geometry.add(TRoadPlanViewGeometry().apply {
            this.x = start.x
            this.y = start.y
            this.s = roadGeometry.offset
            this.length = roadGeometry.length
            this.paramPoly3 = paramPoly3
        })
    }
    return tRoadPlanViewGeometry
}

private fun serializeIntersection(intersection: Intersection): TJunction {
    return TJunction().apply {
        id = intersection.id.toString()
    }
}
