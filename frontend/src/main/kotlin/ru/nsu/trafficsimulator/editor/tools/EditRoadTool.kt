package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import imgui.type.ImInt
import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.findRoad
import ru.nsu.trafficsimulator.math.getIntersectionWithGround
import ru.nsu.trafficsimulator.model.Layout

class EditRoadTool() : IEditingTool {
    private val name = "Edit road"
    private var layout: Layout? = null
    private var camera: Camera? = null

    private var currentLeftLines: ImInt? = null
    private var currentRightLines: ImInt? = null

    override fun getButtonName(): String {
        return name
    }

    override fun handleDown(screenPos: Vec2, button: Int): Boolean {
        return true
    }

    override fun handleUp(screenPos: Vec2, button: Int): IStateChange? {
        val intersection = getIntersectionWithGround(screenPos, camera!!) ?: return null
        val road = findRoad(layout!!, intersection) ?: return null
        return EditRoadStateChange(layout!!, road, currentLeftLines, currentRightLines)
    }

    override fun handleDrag(screenPos: Vec2) {
        return
    }

    override fun render(modelBatch: ModelBatch?) {
        return
    }

    override fun init(layout: Layout, camera: Camera, reset: Boolean) {
        this.camera = camera
        this.layout = layout
    }

    fun setLines(currentLeftLines: ImInt?, currentRightLines: ImInt?) {
        this.currentLeftLines = currentLeftLines
        this.currentRightLines = currentRightLines;
    }
}
