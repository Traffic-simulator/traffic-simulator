package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.glutils.IndexData
import com.badlogic.gdx.graphics.glutils.VertexData

class RoadMesh(val myVertices: VertexData, indices: IndexData, isVertexArray: Boolean) : Mesh(myVertices, indices, isVertexArray) {
    fun updateVerticesImmediately(offset: Int, data: FloatArray, count: Int) {
        if (myVertices is VBOWithVAOBatched) {
            myVertices.updateVerticesImmediately(offset, data, count)
        } else {
            myVertices.updateVertices(offset, data, offset, count)
        }
    }
}
