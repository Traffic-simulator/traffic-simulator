package ru.nsu.trafficsimulator.serializer

import opendrive.ERoadLinkElementType
import opendrive.OpenDRIVE
import opendrive.TJunction
import opendrive.TRoad
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Point
import ru.nsu.trafficsimulator.model.Road

class Deserializer {
    fun deserialize(openDRIVE: OpenDRIVE): Layout {
        val layout = Layout()

        val intersections = openDRIVE.junction
            .map { deserializeIntersection(it) }

        val idToIntersection = intersections.associateBy { it.id }

        val roads = openDRIVE.road
            .filter { it.junction == "-1" }
            .map { deserializeRoad(it, idToIntersection) }

        val intersectionRoads = openDRIVE.road
            .filter { it.junction != "-1" }
            .map { deserializeIntersectionRoad(it, idToIntersection) }

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

//        tRoad.planView.geometry.get

        val road = Road(
            id,
            startIntersection,
            endIntersection,
            tRoad.length,

            )

        return road
    }

    private fun deserializeIntersectionRoad(tRoad: TRoad, idToIntersection: Map<Long, Intersection>): Road {

//        tRoad.


//            val road = Road()
        throw NotImplementedError()

//        return road
    }

    private fun deserializeIntersection(junction: TJunction): Intersection {
        val intersection = Intersection(junction.id.toLong(), Point(0.0, 0.0, 0.0))


        return intersection
    }


}
