package ru.nsu.trafficsimulator.editor.changes

import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.intsettings.BuildingIntersectionSettings
import ru.nsu.trafficsimulator.model.intsettings.BuildingType
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Layout

class AddBuildingStateChange(
    private val start: Intersection,
    private val startDir: Vec3,
    private val end: Vec3,
    private val endDir: Vec3
) : IStateChange {
    private var settings: BuildingIntersectionSettings = BuildingIntersectionSettings(BuildingType.HOME)
    private var buildingIntersection: Intersection? = null
    private var buildingRoad: Road? = null

    override fun apply(layout: Layout) {
        if (buildingIntersection == null) {
            val (intersection, road) = layout.addBuilding(start, startDir, end, endDir, settings)
            buildingIntersection = intersection
            buildingRoad = road
        } else {
            layout.pushRoad(buildingRoad!!)
            layout.pushIntersection(buildingIntersection!!)
            start.connectRoad(buildingRoad!!)
        }
    }

    override fun revert(layout: Layout) {
        if (buildingRoad == null || buildingIntersection == null) {
            return
        }
        start.removeRoad(buildingRoad!!)
        layout.roads.remove(buildingRoad!!.id)
        layout.intersections.remove(buildingIntersection!!.id)
    }
}
