package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.GdxRuntimeException

class RoadModelBuilder : ModelBuilder() {

    companion object {
        var meshBuilders: MutableList<RoadMeshBuilder> = mutableListOf()
        private const val MESH_FULLNESS_THRESHOLD = 0.75
    }

    override fun part(
        id: String,
        primitiveType: Int,
        attributes: VertexAttributes,
        material: Material
    ): MeshPartBuilder {
        val builder = getBuilder(attributes)
        part(builder.part(id, primitiveType), material)
        return builder
    }

    override fun end(): Model {
        for (builder in meshBuilders) {
            builder.end()
        }
        meshBuilders.clear()
        return super.end()
    }

    private fun getBuilder(attributes: VertexAttributes): RoadMeshBuilder {
        if (meshBuilders.isEmpty() || meshBuilders.last().lastIndex() < MeshBuilder.MAX_VERTICES * MESH_FULLNESS_THRESHOLD) {
            meshBuilders.add(RoadMeshBuilder())
        }
        val builder = meshBuilders.last()
        builder.begin(attributes)
        return builder
    }
}
