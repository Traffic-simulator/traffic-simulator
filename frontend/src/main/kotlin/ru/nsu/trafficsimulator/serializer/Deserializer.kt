package ru.nsu.trafficsimulator.serializer

import OpenDriveReader
import javazoom.jl.decoder.SynthesisFilter.deserialize
import opendrive.*
import ru.nsu.trafficsimulator.model.*
import kotlin.math.max

class Deserializer {
    fun deserialize(openDRIVE: OpenDRIVE): Layout {
        val layout = Layout()

        val intersections = openDRIVE.junction
            .map { deserializeIntersection(it) }
        intersections.forEach(layout::pushIntersection)

        val roads = openDRIVE.road
            .filter { it.junction == "-1" }
            .map { deserializeRoad(it, layout.intersections) }
            .forEach(layout::pushRoad)

        val intersectionRoads = openDRIVE.road
            .filter { it.junction != "-1" }
            .map { deserializeIntersectionRoad(it, layout.intersections, layout.roads) }
            .forEach(layout::pushIntersectionRoad)

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
            tRoad.length,
            leftLane,
            rightLane,
            spline
        )

        return road
    }

    private fun deserializeIntersectionRoad(
        tRoad: TRoad,
        idToIntersection: Map<Long, Intersection>,
        idToRoad: Map<Long, Road>
    ): IntersectionRoad {

        val id = tRoad.id.toLong()
        val intersection = idToIntersection[tRoad.junction.toLong()]
            ?: throw IllegalArgumentException("Intersection road have no intersection")
        val from = idToRoad[tRoad.link.predecessor.elementId.toLong()]
            ?: throw IllegalArgumentException("Intersection road have no predecessor")
        val to = idToRoad[tRoad.link.successor.elementId.toLong()]
            ?: throw IllegalArgumentException("Intersection road have no successor")
        val length = tRoad.length
        val spline = planeViewToSpline(tRoad.planView)
        val lanes = max(
            tRoad.lanes.laneSection[0]?.left?.lane?.count { it.type == ELaneType.DRIVING } ?: 0,
            tRoad.lanes.laneSection[0]?.right?.lane?.count { it.type == ELaneType.DRIVING } ?: 0
        )

        return IntersectionRoad(
            id,
            intersection,
            from,
            to,
            length,
            lanes,
            spline
        )
    }

    private fun deserializeIntersection(junction: TJunction): Intersection {
        val intersection = Intersection(junction.id.toLong(), Vec3(0.0, 0.0, 0.0))
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
                    val y = Poly3(av, bv, cv ,dv)
                    val normalized = pRange == EParamPoly3PRange.NORMALIZED
                    spline.addPoly(startPoint, hdg, length, x, y, normalized)
                }
            } else {
                throw NotImplementedError("Unsupported geometry: $geometry")
            }
        }

        return spline
    }
}
