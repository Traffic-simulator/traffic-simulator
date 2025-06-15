package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.GdxRuntimeException

class RoadModelBuilder : ModelBuilder() {
    companion object {
        var meshBuilder: RoadMeshBuilder? = null
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
        meshBuilder?.end()
        meshBuilder = null
        return super.end()
    }

    private fun getBuilder(attributes: VertexAttributes): RoadMeshBuilder {
        if (meshBuilder != null) {
            return meshBuilder as RoadMeshBuilder
        }
        meshBuilder = RoadMeshBuilder()
        meshBuilder!!.begin(attributes)
        return meshBuilder!!
    }
}
