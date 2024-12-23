package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Vec2
import ru.nsu.trafficsimulator.model.getIntersectionWithGround

class DeleteRoadTool : IEditingTool {
    private val name = "Delete Road"
    private var layout: Layout? = null
    private var camera: Camera? = null

    private val roadIntersectionThreshold: Double = 5.0
    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): Boolean {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false
        val road = findRoad(intersection) ?: return false
        layout!!.deleteRoad(road)
        println(layout!!.intersections.size)
        return true
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch?) {
        return
    }

    override fun init(layout: Layout, camera: Camera) {
        this.camera = camera
        this.layout = layout
    }

    private fun findRoad(point: Vector3): Road? {
        var minDistance = Double.MAX_VALUE
        var closestRoad : Road? = null
        val point2d = Vec2(point.x.toDouble(), point.z.toDouble())
        for (road in layout!!.roads.values) {
            val distance = road.geometry.closestPoint(point2d).distance(point2d)
            if (distance < minDistance) {
                minDistance = distance
                closestRoad = road
            }
        }
        if (minDistance < roadIntersectionThreshold) {
            return closestRoad
        } else {
            return null
        }
    }
}
