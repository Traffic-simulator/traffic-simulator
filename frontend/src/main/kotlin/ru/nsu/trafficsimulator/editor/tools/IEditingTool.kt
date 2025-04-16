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
    fun handleDown(screenPos: Vec2, button: Int): Boolean

    /**
     * @return Whether to update layout model
     */
    fun handleUp(screenPos: Vec2, button: Int): IStateChange?
    fun handleDrag(screenPos: Vec2)
    fun render(modelBatch: ModelBatch?)
    fun init(layout: Layout, camera: Camera)
}
