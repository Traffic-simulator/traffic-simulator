package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Building
import ru.nsu.trafficsimulator.model.BuildingType
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class AddBuildingStateChange(
    private val start: Intersection,
    private val startDir: Vec3,
    private val end: Intersection,
    private val endDir: Vec3
) : IStateChange {

    private var building: Building = Building(BuildingType.HOME)

    override fun apply(layout: Layout) {
        layout.addBuilding(start, startDir, end.position.toVec3(), endDir, building)
    }

    override fun revert(layout: Layout) {
        TODO("Not yet implemented")
    }
}
