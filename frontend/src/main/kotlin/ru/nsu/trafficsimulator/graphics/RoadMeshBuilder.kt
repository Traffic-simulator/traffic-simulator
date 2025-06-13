package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import kotlin.math.min

class RoadMeshBuilder : MeshBuilder() {
    override fun end(): Mesh {
        val attributes = ModelGenerator.createAttributes()
        var nextAttributeStart = 0
        for (attribute in attributes) {
            attribute.offset = nextAttributeStart
            nextAttributeStart += attribute.sizeInBytes * numVertices
        }

        val mesh = Mesh(
            true,
            min(numVertices.toDouble(), MAX_VERTICES.toDouble()).toInt(),
            indices.size,
            attributes
        )
        println("$numVertices vertices with ${numVertices * floatsPerVertex} floats")
        for (attribute in attributes) {
            print("${attribute.alias}: ${attribute.offset}")
        }
        println()
        return super.end()
    }
}
