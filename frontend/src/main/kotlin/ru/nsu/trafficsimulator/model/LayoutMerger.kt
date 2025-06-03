package ru.nsu.trafficsimulator.model

const val THRESHOLD = 1.0

class LayoutMerger {

    fun merge(layouts: List<Layout>): Layout {
        val resultLayout = Layout()

        layouts.forEach { layout ->
            layout.roads.values.forEach {
                resultLayout.pushRoad(it, true)
            }
        }

        val mergingIntersections = layouts.map { layout ->
            layout.intersections.values
                .filter { it.isMergingIntersection }
        }
            .flatten()

        val used = mutableSetOf<Long>()
        mergingIntersections.forEach { intersection ->
            if (!used.contains(intersection.id)) {
                intersection.isMergingIntersection = false
                used.add(intersection.id)
                resultLayout.pushIntersection(intersection)

                mergingIntersections.filter { it.id == intersection.id && it !== intersection }
                    .forEach { int ->
                        int.incomingRoads.forEach { road ->
                            road.reconnectIntersection(int, intersection)
                        }
                    }
            }
        }

        layouts.forEach { layout ->
            layout.intersections.values.filter { !it.isMergingIntersection }.forEach {
                resultLayout.pushIntersection(it, true)
            }
        }

        return resultLayout
    }
}
