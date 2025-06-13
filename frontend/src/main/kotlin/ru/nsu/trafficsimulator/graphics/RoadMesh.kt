package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.glutils.IndexData
import com.badlogic.gdx.graphics.glutils.VertexData

class RoadMesh(vertices: VertexData, indices: IndexData, isVertexArray: Boolean) : Mesh(vertices, indices, isVertexArray) {}
