package ru.nsu.trafficsimulator.serializer

import opendrive.*
import ru.nsu.trafficsimulator.math.Poly3
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.IntersectionRoad
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import kotlin.math.cos
import kotlin.math.sin

const val MAX_SPEED = "60"

fun serializeLayout(layout: Layout, newDistrict: Int? = null): OpenDRIVE {
    val openDrive = OpenDRIVE()

    openDrive.addUserData("district", (newDistrict ?: layout.district).toString())


    var irId = 0L
    if (layout.roads.isNotEmpty()) {
        irId = layout.roads.values.maxBy { it.id }.id + 1
    }
    for (intersection in layout.intersections.values) {
        val roads = intersection.intersectionRoads.values.toList()
        intersection.intersectionRoads.clear()
        for (road in roads) {
            val id = irId++
            intersection.intersectionRoads[id] = road.copy(id = id)
            intersection.irNextId = irId
        }
    }

    for (road in layout.roads.values) {
        openDrive.road.add(serializeRoad(road))
    }

    for (intersectionRoad in layout.intersections
        .map { (_, intersection) -> intersection.intersectionRoads.values }
        .flatten()) {
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
        predecessor = TRoadLinkPredecessorSuccessor()
        predecessor.elementType = ERoadLinkElementType.JUNCTION
        predecessor.elementId = road.startIntersection.id.toString()

        successor = TRoadLinkPredecessorSuccessor()
        successor.elementType = ERoadLinkElementType.JUNCTION
        successor.elementId = road.endIntersection.id.toString()
    }

    tRoad.type.add(TRoadType().apply {
        s = 0.0
        type = ERoadType.TOWN
        speed = TRoadTypeSpeed()
        speed.max = road.maxSpeed.toString()
        speed.unit = EUnitSpeed.KM_H
    })

    tRoad.signals = TRoadSignals().apply {
        if (road.startIntersection.hasSignals) {
            val trafficLight = road.startIntersection.signals[road]!!
            signal.add(TRoadSignalsSignal().apply {
                orientation = "+"
                s = 0.0
                dynamic = TYesNo.YES
                subtype =
                    "${trafficLight.redOffsetOnStartSecs}-${trafficLight.redTimeSecs}-${trafficLight.greenTimeSecs}"
                id = road.startIntersection.id.toString()
            })
        }

        if (road.endIntersection.hasSignals) {
            val trafficLight = road.endIntersection.signals[road]!!
            signal.add(TRoadSignalsSignal().apply {
                orientation = "-"
                s = road.length
                dynamic = TYesNo.YES
                subtype =
                    "${trafficLight.redOffsetOnStartSecs}-${trafficLight.redTimeSecs}-${trafficLight.greenTimeSecs}"
                id = road.endIntersection.id.toString()
            })
        }
    }

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
    tRoad.addUserData("startSpline", serializeVec2(road.geometry.getPoint(0.0)))
    tRoad.addUserData("endSpline", serializeVec2(road.geometry.getPoint(road.geometry.length)))
    tRoad.addUserData("startDirectionSpline", serializeVec2(road.geometry.getDirection(0.0)))
    tRoad.addUserData("endDirectionSpline", serializeVec2(road.geometry.getDirection(road.geometry.length)))

    tRoad.addUserData("district", road.district.toString())

    return tRoad
}

private fun serializeIntersectionRoad(road: IntersectionRoad): TRoad {
    val tRoad = TRoad()
    //An intersection road changes id
    tRoad.id = road.id.toString()
    tRoad.length = road.geometry.length
    tRoad.junction = road.intersection.id.toString()

    tRoad.type.add(TRoadType().apply {
        s = 0.0
        type = ERoadType.TOWN
        speed = TRoadTypeSpeed()
        speed.max = MAX_SPEED
        speed.unit = EUnitSpeed.KM_H
    })

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

        val rightLane = TRoadLanesLaneSectionRightLane()
        rightLane.id = (-1).toBigInteger()
        rightLane.type = ELaneType.DRIVING
        rightLane.link = TRoadLanesLaneSectionLcrLaneLink()
        rightLane.link.predecessor.add(TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor().apply {
            id = road.laneLinkage.first.toBigInteger()
        })
        rightLane.link.successor.add(TRoadLanesLaneSectionLcrLaneLinkPredecessorSuccessor().apply {
            id = road.laneLinkage.second.toBigInteger()
        })
        rightLane.borderOrWidth.add(TRoadLanesLaneSectionLrLaneWidth().apply {
            a = Layout.LANE_WIDTH
            b = 0.0
            c = 0.0
            d = 0.0
        })
        right.lane.add(rightLane)

    })

    tRoad.addUserData("district", road.district.toString())

    return tRoad
}

private fun serializeIntersection(intersection: Intersection): TJunction {
    val tJunction = TJunction()

    tJunction.id = intersection.id.toString()

    var connectorId = 0
    for ((_, intersectionRoad) in intersection.intersectionRoads) {
        tJunction.connection.add(TJunctionConnection().apply {
            id = (connectorId++).toString()
            incomingRoad = intersectionRoad.fromRoad.id.toString()
            connectingRoad = intersectionRoad.id.toString()
            contactPoint = EContactPoint.START

            laneLink.add(TJunctionConnectionLaneLink().apply {
                from = intersectionRoad.laneLinkage.first.toBigInteger()
                to = (-1).toBigInteger()
            })

        })
    }
    tJunction.addUserData("position", serializeVec2(intersection.position))
    tJunction.addUserData("padding", intersection.padding.toString())

    intersection.building?.let {
        tJunction.addUserData("buildingType", it.type.toString())
        tJunction.addUserData("buildingCapacity", it.capacity.toString())
        tJunction.addUserData("buildingFullness", it.fullness.toString())
    }

    intersection.merging?.let { merging ->
        tJunction.addUserData("mergingIntersection", "true")
        tJunction.addUserData("firstDistrict", merging.firstDistrict.toString())
        tJunction.addUserData("secondDistrict", merging.secondDistrict.toString())
    }

    intersection.splitSettings?.let { splitting ->
        tJunction.addUserData("splitIntersection", "true")
        tJunction.addUserData("firstDistrict", splitting.firstDistrict.toString())
        tJunction.addUserData("secondDistrict", splitting.secondDistrict.toString())
    }

    return tJunction
}

private fun generateRoadPlaneView(geometry: Spline): TRoadPlanView {
    val tRoadPlanViewGeometry = TRoadPlanView()
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

fun createUserData(key: String, value: String) = TUserData().apply {
    this.code = key
    this.value = value
}

private fun serializeVec2(vec2: Vec2) = "(${vec2.x};${vec2.y})"

private fun TJunction.addUserData(key: String, value: String) =
    this.getGAdditionalData().add(createUserData(key, value))

private fun TRoad.addUserData(key: String, value: String) =
    this.getGAdditionalData().add(createUserData(key, value))

private fun OpenDRIVE.addUserData(key: String, value: String) =
    this.getGAdditionalData().add(createUserData(key, value))


