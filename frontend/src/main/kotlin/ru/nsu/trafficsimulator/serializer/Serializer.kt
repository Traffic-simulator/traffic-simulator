package ru.nsu.trafficsimulator.serializer

import opendrive.*
import ru.nsu.trafficsimulator.math.Poly3
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.model.*
import kotlin.math.cos
import kotlin.math.sin

const val MAX_SPEED = "60"

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
    var geo = road.geometry
    if (road.startPadding != 0.0 || road.endPadding != 0.0) {
        geo = road.geometry.copy(road.startPadding, road.endPadding)
    }

    val tRoad = TRoad()
    tRoad.id = road.id.toString()
    tRoad.length = geo.length
    tRoad.junction = "-1"

    tRoad.link = TRoadLink().apply {
        road.startIntersection?.let {
            predecessor = TRoadLinkPredecessorSuccessor()
            predecessor.elementType = ERoadLinkElementType.JUNCTION
            predecessor.elementId = it.id.toString()
        }

        road.endIntersection?.let {
            successor = TRoadLinkPredecessorSuccessor()
            successor.elementType = ERoadLinkElementType.JUNCTION
            successor.elementId = it.id.toString()
        }
    }

    tRoad.type.add(TRoadType().apply {
        s = 0.0
        type = ERoadType.TOWN
        speed = TRoadTypeSpeed()
        speed.max = MAX_SPEED
        speed.unit = EUnitSpeed.KM_H
    })

    tRoad.planView = generateRoadPlaneView(geo)


    tRoad.lanes = TRoadLanes()
    tRoad.lanes.laneOffset.add(TRoadLanesLaneOffset().apply {
        a = 0.0
        b = 0.0
        c = 0.0
        d = 0.0
        s = 0.0
    })
    tRoad.lanes.laneSection.add(TRoadLanesLaneSection().apply {
        s = 0.0
        left = TRoadLanesLaneSectionLeft()
        for (i in 1..road.leftLane) {
            val leftLane = TRoadLanesLaneSectionLeftLane()
            leftLane.id = i.toBigInteger()
            leftLane.type = ELaneType.DRIVING
            leftLane.borderOrWidth.add(TRoadLanesLaneSectionLrLaneWidth().apply {
                a = Layout.LANE_WIDTH
                b = 0.0
                c = 0.0
                d = 0.0
            })
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
            rightLane.type = ELaneType.DRIVING
            rightLane.borderOrWidth.add(TRoadLanesLaneSectionLrLaneWidth().apply {
                a = Layout.LANE_WIDTH
                b = 0.0
                c = 0.0
                d = 0.0
            })
            right.lane.add(rightLane)
        }
    })

    return tRoad
}

private fun serializeIntersectionRoad(road: IntersectionRoad): TRoad {
    val tRoad = TRoad()
    tRoad.id = road.id.toString()
    tRoad.length = road.geometry.length
    tRoad.junction = road.intersection.id.toString()

    tRoad.link = TRoadLink()
    tRoad.link.predecessor = TRoadLinkPredecessorSuccessor().apply {
        elementType = ERoadLinkElementType.ROAD
        elementId = road.fromRoad.id.toString()
        contactPoint =
            if (road.fromRoad.contact(road.intersection) == Road.Companion.ContactPoint.START) EContactPoint.START else EContactPoint.END
    }
    tRoad.link.successor = TRoadLinkPredecessorSuccessor().apply {
        elementType = ERoadLinkElementType.ROAD
        elementId = road.toRoad.id.toString()
        contactPoint =
            if (road.toRoad.contact(road.intersection) == Road.Companion.ContactPoint.START) EContactPoint.START else EContactPoint.END
    }
    tRoad.planView = generateRoadPlaneView(road.geometry)

    tRoad.lanes = TRoadLanes()
    tRoad.lanes.laneOffset.add(TRoadLanesLaneOffset().apply {
        a = 0.0
        b = 0.0
        c = 0.0
        d = 0.0
        s = 0.0
    })

    tRoad.lanes.laneSection.add(TRoadLanesLaneSection().apply {
        s = 0.0
        center = TRoadLanesLaneSectionCenter()
        val centerLane = TRoadLanesLaneSectionCenterLane()
        centerLane.id = 0.toBigInteger()
        center.lane.add(centerLane)

        right = TRoadLanesLaneSectionRight()
        for (linkage in road.laneLinkage) {
            val rightLane = TRoadLanesLaneSectionRightLane()
            rightLane.id = (-linkage.second).toBigInteger()
            rightLane.type = ELaneType.DRIVING
            rightLane.link = TRoadLanesLaneSectionLcrLaneLink()
            rightLane.link.predecessor.add(TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor().apply {
                id = linkage.first.toBigInteger()
            })
            rightLane.link.successor.add(TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor().apply {
                id = linkage.third.toBigInteger()
            })
            rightLane.borderOrWidth.add(TRoadLanesLaneSectionLrLaneWidth().apply {
                a = Layout.LANE_WIDTH
                b = 0.0
                c = 0.0
                d = 0.0
            })
            right.lane.add(rightLane)
        }
    })

    return tRoad
}

private fun serializeIntersection(intersection: Intersection): TJunction {
    val tJunction = TJunction()

    tJunction.id = intersection.id.toString()

    var connectorId = 0
    for (intersectionRoad in intersection.intersectionRoads) {
        tJunction.connection.add(TJunctionConnection().apply {
            id = (connectorId++).toString()
            incomingRoad = intersectionRoad.fromRoad.id.toString()
            connectingRoad = intersectionRoad.id.toString()
            contactPoint = EContactPoint.START
            for (linkage in intersectionRoad.laneLinkage) {
                laneLink.add(TJunctionConnectionLaneLink().apply {
                    from = linkage.first.toBigInteger()
                    to = -linkage.second.toBigInteger()
                })
            }
        })
    }

    return tJunction
}

private fun generateRoadPlaneView(geometry: Spline): TRoadPlanView {
    val tRoadPlanViewGeometry = TRoadPlanView()
    println(geometry)
    for (roadGeometry in geometry.splineParts) {
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
            pRange = if (roadGeometry.normalized) EParamPoly3PRange.NORMALIZED else EParamPoly3PRange.ARC_LENGTH
        }

        tRoadPlanViewGeometry.geometry.add(TRoadPlanViewGeometry().apply {
            this.x = start.x
            this.y = start.y
            this.hdg = -rotAngle
            this.s = roadGeometry.offset
            this.length = roadGeometry.length
            this.paramPoly3 = paramPoly3
        })
    }
    return tRoadPlanViewGeometry
}
