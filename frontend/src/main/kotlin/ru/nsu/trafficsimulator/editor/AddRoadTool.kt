package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.*

class AddRoadTool : IEditingTool {
    private val name = "Add Road"
    private var layout: Layout? = null
    private var camera: Camera? = null
    private val startDirectionLength = 25.0

    private val selectedPositions = arrayOfNulls<Vec3>(2)
    private var selectedPositionCount = 0
    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) return true
        val intersectionPoint = getIntersectionWithGround(screenPos, camera!!) ?: return true
        selectedPositions[selectedPositionCount] = Vec3(intersectionPoint)
        selectedPositionCount += 1
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        if (selectedPositionCount != 2) return null
        selectedPositionCount = 0
        if (selectedPositions[0] == selectedPositions[1]) return null

        val dir = (selectedPositions[0]!! - selectedPositions[1]!!).normalized()
        val startDirection = selectedPositions[0]!! + dir * startDirectionLength
        val endDirection = selectedPositions[1]!! + dir * startDirectionLength

        return AddRoadStateChange(layout!!, selectedPositions[0]!!, startDirection, selectedPositions[1]!!, endDirection)
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch?) {
        return
    }

    override fun init(layout: Layout, camera: Camera) {
        this.layout = layout
        this.camera = camera
        selectedPositionCount = 0
    }
}
