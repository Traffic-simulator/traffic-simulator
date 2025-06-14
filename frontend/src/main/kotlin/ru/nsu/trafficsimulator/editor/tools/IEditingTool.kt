package ru.nsu.trafficsimulator.editor.tools

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import ru.nsu.trafficsimulator.editor.changes.IStateChange
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.math.Vec2

interface IEditingTool {
    fun getButtonName(): String

    /**
     * @return Whether to disable camera controls until mouse up
     */
    fun handleDown(screenPos: Vec2, button: Int): Boolean {
        return false
    }

    /**
     * @return Whether to update layout model
     */
    fun handleUp(screenPos: Vec2, button: Int): IStateChange?

    fun handleDrag(screenPos: Vec2) {
        return
    }

    fun runImgui(): IStateChange? {
       return null
    }

    fun render(modelBatch: ModelBatch) {
        return
    }

    /**
     * @param reset - whether the state should be reset completely or just updated
     */
    fun init(layout: Layout, camera: Camera, reset: Boolean)
}
