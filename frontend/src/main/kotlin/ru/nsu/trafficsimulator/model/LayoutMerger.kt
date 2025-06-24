package ru.nsu.trafficsimulator.model

import OpenDriveWriter
import ru.nsu.trafficsimulator.model.intsettings.SplitDistrictsIntersectionSettings
import ru.nsu.trafficsimulator.serializer.serializeLayout

const val THRESHOLD = 1.0

class LayoutMerger {

    fun merge(layouts: List<Layout>): Layout {
        val newDistrict = layouts.maxBy { it.district }.district + 1

        val resultLayout = Layout(newDistrict)

        layouts.forEach { layout ->
            layout.roads.values.forEach {
                resultLayout.pushRoad(it, true)
            }
        }

        val mergingIntersections = layouts.map { layout ->
            layout.intersections.values
                .filter { it.isMerging }
        }.flatten()

        layouts.forEach { layout ->
            layout.intersections.values.filter { !it.isMerging }.forEach {
                resultLayout.pushIntersection(it, true)
            }
        }

        val used = mutableSetOf<Long>()
        mergingIntersections.forEach { intersection ->
            if (!used.contains(intersection.id)) {
                val merging = intersection.merging!!
                intersection.intersectionSettings = SplitDistrictsIntersectionSettings(
                    merging.firstDistrict,
                    merging.secondDistrict
                )

                val newIntersection = Intersection(
                    resultLayout.intersectionIdCount++, intersection.position,
                    intersectionSettings = SplitDistrictsIntersectionSettings(
                        merging.firstDistrict,
                        merging.secondDistrict
                    )
                )

                used.add(intersection.id)
                resultLayout.pushIntersection(newIntersection, true)

                mergingIntersections.filter { it.id == intersection.id }
                    .forEach { int ->
                        int.incomingRoads.forEach { road ->
                            road.reconnectIntersection(int, newIntersection)
                        }
                    }

                resultLayout.moveIntersection(newIntersection, newIntersection.position.toVec3())
            }
        }

        return resultLayout
    }
}
