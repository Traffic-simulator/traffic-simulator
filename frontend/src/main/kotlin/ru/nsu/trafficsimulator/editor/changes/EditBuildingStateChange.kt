package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.model.intsettings.BuildingType
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class EditBuildingStateChange(
    private val intersection: Intersection,
    private var currentCapacity: Int,
    private var currentType: String
) : IStateChange {
    private val prevBuildingCapacity = intersection.building!!.capacity
    private val prevBuildingType = intersection.building!!.type.toString()

    override fun apply(layout: Layout) {
        intersection.building!!.capacity = currentCapacity
        intersection.building!!.type = BuildingType.valueOf(currentType)
    }

    override fun revert(layout: Layout) {
        intersection.building!!.capacity = prevBuildingCapacity
        intersection.building!!.type = BuildingType.valueOf(prevBuildingType)
    }
}
