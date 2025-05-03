package ru.nsu.trafficsimulator.serializer

import opendrive.*
import ru.nsu.trafficsimulator.math.Poly3
import ru.nsu.trafficsimulator.math.Spline
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.*
import ru.nsu.trafficsimulator.model.Layout.Companion.DEFAULT_INTERSECTION_PADDING
import kotlin.math.max

class Deserializer {
    companion object {
        fun deserialize(openDRIVE: OpenDRIVE): Layout {
            val layout = Layout()

            val intersections = openDRIVE.junction
                .map { deserializeIntersection(it) }
            intersections.forEach { pushIntersection(it, layout) }

            val roads = openDRIVE.road
                .filter { it.junction == "-1" }
                .map { deserializeRoad(it, layout.intersections) }
            roads.forEach { pushRoad(it, layout) }

            val intersectionRoads = openDRIVE.road
                .filter { it.junction != "-1" }
                .map { deserializeIntersectionRoad(it, layout.intersections, layout.roads) }
            intersectionRoads.forEach { pushIntersectionRoad(it, layout) }

            recalculateIntersectionPosition(layout)

            return layout
        }


        private fun deserializeRoad(tRoad: TRoad, idToIntersection: Map<Long, Intersection>): Road {
            val id = tRoad.id.toLong()

            var startIntersection: Intersection? = null
            var endIntersection: Intersection? = null

            // TODO add support for connections between roads
            tRoad.link?.predecessor?.let {
                if (it.elementType == ERoadLinkElementType.JUNCTION) {
                    startIntersection = idToIntersection[it.elementId.toLong()]
                }
            }
            tRoad.link?.successor?.let {
                if (it.elementType == ERoadLinkElementType.JUNCTION) {
                    endIntersection = idToIntersection[it.elementId.toLong()]
                }
            }

            val leftLane = tRoad.lanes.laneSection[0]?.left?.lane?.count { it.type == ELaneType.DRIVING } ?: 0
            val rightLane = tRoad.lanes.laneSection[0]?.right?.lane?.count { it.type == ELaneType.DRIVING } ?: 0

            val spline = planeViewToSpline(tRoad.planView)


            val road = Road(
                id,
                startIntersection,
                endIntersection,
                leftLane,
                rightLane,
                spline
            )

            road.endIntersection?.incomingRoads?.add(road)
            road.startIntersection?.incomingRoads?.add(road)

            return road
        }

        private fun deserializeIntersectionRoad(
            tRoad: TRoad,
            idToIntersection: Map<Long, Intersection>,
            idToRoad: Map<Long, Road>
        ): IntersectionRoad {
            val intersection = idToIntersection[tRoad.junction.toLong()]
            val lanes = max(
                tRoad.lanes.laneSection[0]?.left?.lane?.count { it.type == ELaneType.DRIVING } ?: 0,
                tRoad.lanes.laneSection[0]?.right?.lane?.count { it.type == ELaneType.DRIVING } ?: 0
            )
            val intersectionRoad = IntersectionRoad(
                id = tRoad.id.toLong(),
                intersection = intersection
                    ?: throw IllegalArgumentException("Intersection road have no intersection"),
                fromRoad = idToRoad[tRoad.link.predecessor.elementId.toLong()]
                    ?: throw IllegalArgumentException("Intersection road have no predecessor"),
                toRoad = idToRoad[tRoad.link.successor.elementId.toLong()]
                    ?: throw IllegalArgumentException("Intersection road have no successor"),
                lane = lanes,
                geometry = planeViewToSpline(tRoad.planView)
            )

            intersection.intersectionRoads.add(intersectionRoad)
            return intersectionRoad
        }

        private fun deserializeIntersection(junction: TJunction): Intersection {
            val intersection = Intersection(junction.id.toLong(), Vec2(0.0, 0.0))
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
                val red = "\u001b[31m"
                val reset = "\u001b[0m"
                println("${red}WARNING: Full editing of loaded layout is not supported. Beware.${reset}")
            }

            return spline
        }

        private fun recalculateIntersectionPosition(layout: Layout) {
            for (intersection in layout.intersections.values) {
                var pos = Vec2(0.0, 0.0)
                for (road in intersection.incomingRoads) {
                    if (road.startIntersection == intersection) {
                        pos += road.geometry.getPoint(0.0)
                    } else {
                        pos += road.geometry.getPoint(road.geometry.length)
                    }
                }
                intersection.position = pos / intersection.incomingRoads.size.toDouble()
                println(intersection.position)
                println(intersection.intersectionRoads.size)
            }
        }

        fun pushRoad(road: Road, layout: Layout) {
            if (layout.roads.containsKey(road.id) || layout.intersectionRoads.containsKey(road.id)) {
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
            if (layout.roads.containsKey(road.id) || layout.intersectionRoads.containsKey(road.id)) {
                throw IllegalArgumentException("Road with id ${road.id} already exists.")
            }
            if (road.id > layout.roadIdCount) {
                layout.roadIdCount = road.id + 1
            }
            layout.intersectionRoads[road.id] = road
        }
    }
}
