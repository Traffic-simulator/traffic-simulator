package ru.nsu.trafficsimulator.editor.actions

import ru.nsu.trafficsimulator.model.Layout

interface IAction {
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
