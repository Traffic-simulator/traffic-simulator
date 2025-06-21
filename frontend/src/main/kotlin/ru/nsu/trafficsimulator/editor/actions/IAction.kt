package ru.nsu.trafficsimulator.editor.actions

import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.server.Client

interface IAction {
    /**
     * Simply: do we need to update mesh for layout or not?
     */
    fun isStructuralAction(): Boolean

    /**
     * Function to draw a menu
     * @return whether to run the action
     */
    fun runImgui(): Boolean

    /**
     * Do the action if runImgui returned true
     * @return whether layout changed or not
     */
    fun runAction(layout: Layout): Boolean
}
