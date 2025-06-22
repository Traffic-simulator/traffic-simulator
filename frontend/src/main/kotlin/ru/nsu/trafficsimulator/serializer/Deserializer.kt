package ru.nsu.trafficsimulator.serializer

import opendrive.*
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.math.Poly3
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.model.intsettings.BuildingIntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.BuildingType
import ru.nsu.trafficsimulator.model.intsettings.MergingIntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.SplitDistrictsIntersectionSettings
import kotlin.math.abs
import kotlin.math.max

private val INVALID_INTERSECTION_POSITION = Vec2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

class Deserializer {
    companion object {
        fun deserialize(openDRIVE: OpenDRIVE): Layout {
            val userParameters: MutableMap<String, String> = mutableMapOf()
            openDRIVE.getGAdditionalData().forEach { data ->
                val userData = data as TUserData
                userParameters[userData.code] = userData.value
            }

            val district = userParameters["district"]?.toInt() ?: 0
            val layout = Layout(district)

            val intersections = openDRIVE.junction
                .map { deserializeIntersection(it) }
            intersections.forEach { pushIntersection(it, layout) }

            val roads = openDRIVE.road
                .filter { it.junction == "-1" }
                .map { deserializeRoad(it, layout.intersections, district) }
            roads.forEach { pushRoad(it, layout) }
            layout.intersectionIdCount = layout.intersections.keys.max() + 1

            val intersectionRoads = openDRIVE.road
                .filter { it.junction != "-1" }
                .map { deserializeIntersectionRoad(it, layout.intersections, layout.roads, district) }
            intersectionRoads.forEach { pushIntersectionRoad(it, layout) }

            recalculateIntersectionPosition(layout)

            ensureFullSignals(layout)

            return layout
        }


        private fun deserializeRoad(
            tRoad: TRoad,
            idToIntersection: MutableMap<Long, Intersection>,
            layoutDistrict: Int
        ): Road {
            val userParameters: MutableMap<String, String> = mutableMapOf()
            tRoad.getGAdditionalData().forEach { data ->
                val userData = data as TUserData
                userParameters[userData.code] = userData.value
            }

            val id = tRoad.id.toLong()

            var nextId = idToIntersection.keys.max() + 1
            // TODO add support for connections between roads
            val startIntersection = tRoad.link?.predecessor?.let {
                if (it.elementType == ERoadLinkElementType.JUNCTION) {
                    idToIntersection[it.elementId.toLong()]
                } else {
                    logger.warn("Road is connected with road. Import is probably incorrect")
                    null
                }
            } ?: run {
                // Position and padding will be calculated later
                idToIntersection[nextId] = Intersection(nextId, INVALID_INTERSECTION_POSITION)
                idToIntersection[nextId++]!!
            }
            val endIntersection = tRoad.link?.successor?.let {
                if (it.elementType == ERoadLinkElementType.JUNCTION) {
                    idToIntersection[it.elementId.toLong()]
                } else {
                    logger.warn("Road is connected with road. Import is probably incorrect")
                    null
                }
            } ?: run {
                // Position and padding will be calculated later
                idToIntersection[nextId] = Intersection(nextId, Vec2(0.0, 0.0))
                idToIntersection[nextId]!!
            }

            val leftLane = tRoad.lanes.laneSection[0]?.left?.lane?.count { it.type == ELaneType.DRIVING } ?: 0
            val rightLane = tRoad.lanes.laneSection[0]?.right?.lane?.count { it.type == ELaneType.DRIVING } ?: 0

            val spline: Spline =
                if (userParameters.containsKey("startSpline") && userParameters.containsKey("endSpline")
                    && userParameters.containsKey("startDirectionSpline") && userParameters.containsKey("endDirectionSpline")
                ) {
                    val start = parseVec2(userParameters["startSpline"]!!)
                    val end = parseVec2(userParameters["endSpline"]!!)
                    val startDirection = parseVec2(userParameters["startDirectionSpline"]!!)
                    val endDirection = parseVec2(userParameters["endDirectionSpline"]!!)
                    val s = Spline()
                    s.addSplinePart(start to start + startDirection, end to end + endDirection)
                    s
                } else planeViewToSpline(tRoad.planView)


            val road = Road(
                id,
                startIntersection,
                endIntersection,
                leftLane,
                rightLane,
                spline,
                userParameters["district"]?.toInt() ?: layoutDistrict
            )

            // See https://publications.pages.asam.net/standards/ASAM_OpenDRIVE/ASAM_OpenDRIVE_Specification/latest/specification/14_signals/14_01_introduction.html

            tRoad.signals?.signal?.filter { it.dynamic == TYesNo.YES }?.forEach { signal ->
                val trafficLight = Signal()
                val nums = signal.subtype.split("-")
                if (nums.size != 3) {
                    logger.error("Invalid dynamic signal found: Number of numbers is not 3")
                    return@forEach
                }

                trafficLight.redOffsetOnStartSecs = nums[0].toInt()
                trafficLight.redTimeSecs = nums[1].toInt()
                trafficLight.greenTimeSecs = nums[2].toInt()

                if (signal.orientation == "-") {
                    if (abs(signal.s - road.geometry.length) > 1e-2) {
                        logger.error("Invalid traffic light: it's not at the end: ${signal.s} <-> ${road.geometry.length}")
                        return@forEach
                    }
                    endIntersection.signals[road] = trafficLight
                } else {
                    if (abs(signal.s) > 1e-2) {
                        logger.error("Invalid traffic light: it's not at the start: ${signal.s} <-> 0.0")
                        return@forEach
                    }
                    startIntersection.signals[road] = trafficLight
                }
            }

            road.endIntersection.incomingRoads.add(road)
            road.startIntersection.incomingRoads.add(road)

            return road
        }

        private fun deserializeIntersectionRoad(
            tRoad: TRoad,
            idToIntersection: Map<Long, Intersection>,
            idToRoad: Map<Long, Road>,
            layoutDistrict: Int
        ): IntersectionRoad {
            val userParameters: MutableMap<String, String> = mutableMapOf()
            tRoad.getGAdditionalData().forEach { data ->
                val userData = data as TUserData
                userParameters[userData.code] = userData.value
            }

            val intersection = idToIntersection[tRoad.junction.toLong()]

            val leftLanes = tRoad.lanes.laneSection[0]?.left?.lane?.count { it.type == ELaneType.DRIVING } ?: 0
            val rightLanes = tRoad.lanes.laneSection[0]?.right?.lane?.count { it.type == ELaneType.DRIVING } ?: 0
            if (rightLanes != 1 || leftLanes != 0) {
                throw IllegalArgumentException("Intersection road mast have only 1 right lane")
            }

            val link = tRoad.lanes.laneSection[0]?.right?.lane?.get(0)?.link
                ?: throw IllegalArgumentException("Cant find right link")

            val intersectionRoad = IntersectionRoad(
                id = tRoad.id.toLong(),
                intersection = intersection
                    ?: throw IllegalArgumentException("Intersection road have no intersection"),
                fromRoad = idToRoad[tRoad.link.predecessor.elementId.toLong()]
                    ?: throw IllegalArgumentException("Intersection road have no predecessor"),
                toRoad = idToRoad[tRoad.link.successor.elementId.toLong()]
                    ?: throw IllegalArgumentException("Intersection road have no successor"),
                geometry = planeViewToSpline(tRoad.planView),
                laneLinkage = link.predecessor[0].id.toInt() to link.successor[0].id.toInt(),
                district = userParameters["district"]?.toInt() ?: layoutDistrict
            )

            intersection.intersectionRoads[intersectionRoad.id] = intersectionRoad
            return intersectionRoad
        }

        private fun deserializeIntersection(junction: TJunction): Intersection {
            val userParameters: MutableMap<String, String> = mutableMapOf()
            junction.getGAdditionalData().forEach { data ->
                val userData = data as TUserData
                userParameters[userData.code] = userData.value
            }

            val intersection = Intersection(
                junction.id.toLong(),
                position = INVALID_INTERSECTION_POSITION,
                intersectionSettings = null
            )

            userParameters["position"]?.let {
                intersection.position = parseVec2(it)
            }

            userParameters["padding"]?.let {
                intersection.padding = it.toDouble()
            }

            if (userParameters.containsKey("buildingType")) {
                intersection.intersectionSettings =
                    BuildingIntersectionSettings(BuildingType.valueOf(userParameters["buildingType"]!!)).apply {
                        capacity = userParameters["buildingCapacity"]!!.toInt()
                    }
            }

            userParameters["mergingIntersection"]?.let { isMerging ->
                if (isMerging.toBoolean()) {
                    intersection.intersectionSettings =
                        MergingIntersectionSettings(
                            userParameters["firstDistrict"]?.toInt()
                                ?: throw IllegalArgumentException("There is no first district"),
                            userParameters["secondDistrict"]?.toInt()
                                ?: throw IllegalArgumentException("There is no second district")
                        )
                }
            }

            userParameters["splitIntersection"]?.let { isSplit ->
                if (isSplit.toBoolean()) {
                    intersection.intersectionSettings =
                        SplitDistrictsIntersectionSettings(
                            userParameters["firstDistrict"]?.toInt()
                                ?: throw IllegalArgumentException("There is no first district"),
                            userParameters["secondDistrict"]?.toInt()
                                ?: throw IllegalArgumentException("There is no second district")
                        )
                }
            }

            return intersection
        }

        fun planeViewToSpline(planView: TRoadPlanView): Spline {
            val spline = Spline()

            for (geometry in planView.geometry) {
                val startPoint = Vec2(geometry.x, geometry.y)
                val hdg = geometry.hdg
                val length = geometry.length

                if (geometry.line != null) {
                    spline.addLine(startPoint, hdg, length)
                } else if (geometry.arc != null) {
                    spline.addArc(startPoint, hdg, geometry.arc.curvature, length)
                } else if (geometry.paramPoly3 != null) {
                    with(geometry.paramPoly3) {
                        val x = Poly3(au, bu, cu, du)
                        val y = Poly3(av, bv, cv, dv)
                        val normalized = (pRange == EParamPoly3PRange.NORMALIZED)
                        spline.addParamPoly(startPoint, hdg, length, x, y, normalized)
                    }
                } else if (geometry.spiral != null) {
                    spline.addSpiral(startPoint, hdg, geometry.spiral.curvStart, geometry.spiral.curvEnd, length)
                } else {
                    throw NotImplementedError("Unsupported geometry: $geometry")
                }
            }

            if (spline.splineParts.size > 1) {
                logger.warn("Full editing of loaded layout is not supported. Beware.")
            }

            return spline
        }

        private fun recalculateIntersectionPosition(layout: Layout) {
            for (intersection in layout.intersections.values) {
                if (intersection.position != INVALID_INTERSECTION_POSITION) {
                    continue
                }
                var pos = Vec2(0.0, 0.0)
                for (road in intersection.incomingRoads) {
                    pos += if (road.startIntersection == intersection) {
                        road.geometry.getPoint(0.0)
                    } else {
                        road.geometry.getPoint(road.geometry.length)
                    }
                }
                intersection.position = pos / intersection.incomingRoads.size.toDouble()
            }
        }

        fun pushRoad(road: Road, layout: Layout) {
            if (layout.roads.containsKey(road.id)) {
                throw IllegalArgumentException("Road with id ${road.id} already exists.")
            }
            if (road.id > layout.roadIdCount) {
                layout.roadIdCount = road.id + 1
            }
            layout.roads[road.id] = road
        }

        fun pushIntersection(intersection: Intersection, layout: Layout) {
            if (layout.roads.containsKey(intersection.id)) {
                throw IllegalArgumentException("Intersection with id ${intersection.id} already exists.")
            }
            if (intersection.id > layout.roadIdCount) {
                layout.roadIdCount = intersection.id + 1
            }
            layout.intersections[intersection.id] = intersection
            layout.intersectionIdCount = max(layout.intersectionIdCount, intersection.id + 1)
        }

        fun pushIntersectionRoad(road: IntersectionRoad, layout: Layout) {
            if (road.id > layout.roadIdCount) {
                layout.roadIdCount = road.id + 1
            }
            road.intersection.intersectionRoads[road.id] = road
        }

        private fun ensureFullSignals(layout: Layout) {
            for (intersection in layout.intersections.values) {
                if (!intersection.hasSignals) {
                    continue
                }

                for (road in intersection.incomingRoads) {
                    if (!intersection.signals.containsKey(road)) {
                        logger.error("Road $road doesn't have a signal at intersection $intersection; Using default")
                        intersection.signals[road] = Signal()
                    }
                    logger.info("Road $road;$intersection: ${intersection.signals[road]}")
                }
            }
        }
    }
}

private fun parseVec2(str: String): Vec2 {
    val parts = str.replace("(", "")
        .replace(")", "")
        .split(";")

    if (parts.size != 2) {
        throw IllegalArgumentException("Vec2 must be in the format (x;y). Input: $str")
    }

    return Vec2(parts[0].toDouble(), parts[1].toDouble())
}
