package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.model.*

class DeleteRoadTool : IEditingTool {
    private val name = "Delete Road"
    private var layout: Layout? = null
    private var camera: Camera? = null

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): Boolean {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return false
        val road = findRoad(layout!!, intersection) ?: return false
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
}
