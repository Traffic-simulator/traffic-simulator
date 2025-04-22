package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
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

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return null
        val road = findRoad(layout!!, intersection) ?: return null
        return DeleteRoadStateChange(road)
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
