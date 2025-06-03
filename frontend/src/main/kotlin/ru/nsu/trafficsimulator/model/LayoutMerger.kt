package ru.nsu.trafficsimulator.model

class LayoutMerger {
    fun merge(layouts: List<Layout>): Layout {
        val hashMap = hashMapOf<Long, Intersection>()

        layouts.forEach { layout ->
            layout.intersections.values.filter { it.isMergingIntersection }.forEach {
                val id = it.id
                if (!hashMap.containsKey(id)) {
                    hashMap[id] = it
                } else if (

                )
            }
        }
        return Layout()

    }
}
