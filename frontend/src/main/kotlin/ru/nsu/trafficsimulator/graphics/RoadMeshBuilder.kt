package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.glutils.IndexBufferObject
import kotlin.math.min

class RoadMeshBuilder : MeshBuilder() {
    override fun end(): Mesh {
        val attributes = ModelGenerator.createAttributes()

        val vertices = VBOWithVAOBatched(false, numVertices, attributes)
        val indices = IndexBufferObject(false, numIndices)
        val mesh = RoadMesh(vertices, indices, false)
//        println("$numVertices vertices with ${numVertices * floatsPerVertex} floats")
        return super.end(mesh)
    }
}

fun Mesh.constructor() {

}
