package ru.nsu.trafficsimulator.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import mu.KotlinLogging

fun createSphere(color: Color, radius: Double = 5.0): Model {
    val modelBuilder = ModelBuilder()
    val material = Material(ColorAttribute.createDiffuse(color))
    val sphere = modelBuilder.createSphere(
        radius.toFloat(),
        radius.toFloat(),
        radius.toFloat(),
        10,
        10,
        material,
        (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
    )
    return sphere
}
