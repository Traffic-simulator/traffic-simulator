package ru.nsu.trafficsimulator.model_generation

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.math.Vector3
import ktx.math.unaryMinus
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Vec2
import ru.nsu.trafficsimulator.model.Vec3
import kotlin.math.abs
import kotlin.math.floor

class ModelGenerator {
    companion object {
        private val roadHeight = 1.0
        public val laneWidth = 3.5
        private val splineRoadSegmentLen = 2.0f
        private val upVec = Vec3(0.0, roadHeight, 0.0)

        fun createLayoutModel(layout: Layout): Model {
            val modelBuilder = ModelBuilder()
            var meshPartBuilder: MeshPartBuilder
            modelBuilder.begin()
            for (road in layout.roads.values) {
                val node = modelBuilder.node()
                node.translation.set(0.0f, 0.0f, 0.0f)
                meshPartBuilder = modelBuilder.part("road${road.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())


                val hasStart = road.startIntersection?.intersectionRoads?.size.let {
                    it != null && it > 0
                }
                val hasEnd = road.endIntersection?.intersectionRoads?.size.let {
                    it != null && it > 0
                }
                val length = road.geometry.length - if (hasStart) { road.startIntersection!!.padding } else { 0.0 } - if (hasEnd) { road.startIntersection!!.padding } else { 0.0 }

                val start = if (hasStart) { road.startIntersection!!.padding } else { 0.0 }

                val stepCount = floor(length / splineRoadSegmentLen).toInt()
                var prevPos = road.geometry.getPoint(start).toVec3()
                var prevDir = road.geometry.getDirection(start).toVec3().normalized()
                var prevRight = prevDir.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
                var prevLeft = -prevDir.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
                meshPartBuilder.rect(
                    (prevPos + prevLeft).toGdxVec(),
                    (prevPos + prevRight).toGdxVec(),
                    (prevPos + prevRight + upVec).toGdxVec(),
                    (prevPos + prevLeft + upVec).toGdxVec(),
                    -prevDir.toGdxVec()
                )


                for (i in 1..stepCount) {
                    val t = start + i * splineRoadSegmentLen.toDouble()
                    val pos = road.geometry.getPoint(t).toVec3()
                    val direction = road.geometry.getDirection(t).toVec3().normalized()
                    val right = direction.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
                    val left = -direction.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
                    if (direction.dot(prevDir) < 0.0) {
                        prevLeft = prevRight.also { prevRight = prevLeft }
                    }
                    // Top
                    meshPartBuilder.rect(
                        (prevPos + prevRight + upVec).toGdxVec(),
                        (pos + right + upVec).toGdxVec(),
                        (pos + left + upVec).toGdxVec(),
                        (prevPos + prevLeft + upVec).toGdxVec(),
                        Vec3.UP.toGdxVec()
                    )
                    // Right
                    meshPartBuilder.rect(
                        (prevPos + prevRight).toGdxVec(),
                        (pos + right).toGdxVec(),
                        (pos + right + upVec).toGdxVec(),
                        (prevPos + prevRight + upVec).toGdxVec(),
                        (pos - prevPos).cross(Vec3.UP).toGdxVec()
                    )

                    // Right
                    meshPartBuilder.rect(
                        (pos + left).toGdxVec(),
                        (prevPos + prevLeft).toGdxVec(),
                        (prevPos + prevLeft + upVec).toGdxVec(),
                        (pos + left + upVec).toGdxVec(),
                        -(pos - prevPos).cross(Vec3.UP).toGdxVec()
                    )
                    prevPos = pos
                    prevLeft = left
                    prevRight = right
                    prevDir = direction
                }
                val t = road.geometry.length - if (hasEnd) { road.startIntersection!!.padding } else { 0.0 }
                val pos = road.geometry.getPoint(t).toVec3()
                val direction = road.geometry.getDirection(t).toVec3().normalized()
                val right = direction.cross(Vec3.UP).normalized() * laneWidth * road.rightLane.toDouble()
                val left = -direction.cross(Vec3.UP).normalized() * laneWidth * road.leftLane.toDouble()
                meshPartBuilder.rect(
                    (prevPos + prevRight + upVec).toGdxVec(),
                    (pos + right + upVec).toGdxVec(),
                    (pos + left + upVec).toGdxVec(),
                    (prevPos + prevLeft + upVec).toGdxVec(),
                    Vec3.UP.toGdxVec()
                )
                // Right
                meshPartBuilder.rect(
                    (prevPos + prevRight).toGdxVec(),
                    (pos + right).toGdxVec(),
                    (pos + right + upVec).toGdxVec(),
                    (prevPos + prevRight + upVec).toGdxVec(),
                    (pos - prevPos).cross(Vec3.UP).toGdxVec()
                )

                // Right
                meshPartBuilder.rect(
                    (pos + left).toGdxVec(),
                    (prevPos + prevLeft).toGdxVec(),
                    (prevPos + prevLeft + upVec).toGdxVec(),
                    (pos + left + upVec).toGdxVec(),
                    -(pos - prevPos).cross(Vec3.UP).toGdxVec()
                )

                meshPartBuilder.rect(
                    (pos + right).toGdxVec(),
                    (pos + left).toGdxVec(),
                    (pos + left + upVec).toGdxVec(),
                    (pos + right + upVec).toGdxVec(),
                    direction.toGdxVec()
                )
            }

            val intersectionBoxSize = 30.0
            val samplePerSide = 40
            val cellSize = intersectionBoxSize / (samplePerSide - 1).toDouble()
            val upDir = Vec3(0.0, 1.0, 0.0)
            for (intersection in layout.intersections.values) {
                val node = modelBuilder.node()
                node.translation.set(intersection.position.toGdxVec())
                meshPartBuilder = modelBuilder.part("intersection${intersection.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
                val intersectionSdf = { local: Vec3 ->
                    val point = intersection.position + local
                    var minDist = intersectionBoxSize * intersectionBoxSize
                    var laneCount = 1
                    for (road in intersection.intersectionRoads) {
                        val proj = Vec2(point.x, point.z)
                        val dist = (road.geometry.closestPoint(proj) - proj).length()
                        if (abs(dist) < abs(minDist)) {
                            minDist = dist
                            laneCount = 1
                        }
                    }
                    minDist - laneWidth * laneCount
                }
                val insertRect = { a: Vec3, b: Vec3, normal: Vec3 ->
                    meshPartBuilder.rect(
                        a.toGdxVec(),
                        (a + upVec).toGdxVec(),
                        (b + upVec).toGdxVec(),
                        b.toGdxVec(),
                        normal.toGdxVec()
                    )
                }
                val insertTriangle = { a: Vec3, b: Vec3, c: Vec3, normal: Vec3 ->
                    meshPartBuilder.triangle(
                        MeshPartBuilder.VertexInfo().set(
                            a.toGdxVec(),
                            normal.toGdxVec(),
                            null,
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            b.toGdxVec(),
                            normal.toGdxVec(),
                            null,
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            c.toGdxVec(),
                            normal.toGdxVec(),
                            null,
                            null
                        ),
                    )
                }
                val getRefinedGuess = { a: Vec3, b: Vec3 ->
                    val iterationCount = 5
                    var guess = (a + b) / 2.0
                    var left = a
                    var right = b
                    var guessType = intersectionSdf(guess) > 0
                    val leftType = intersectionSdf(left) > 0
                    val rightType = intersectionSdf(right) > 0
                    assert(leftType != rightType)
                    for (i in 0..<iterationCount) {
                        if (guessType == rightType) {
                            right = guess
                            guess = (left + guess) / 2.0
                        } else {
                            left = guess
                            guess = (right + guess) / 2.0
                        }
                        guessType = intersectionSdf(guess) > 0
                    }
                    guess
                }
                val leftBottomCorner = Vec3(-intersectionBoxSize / 2.0, 0.0, -intersectionBoxSize / 2.0)
                val patterns = arrayOf(
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and !bType and !cType and !dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        val abPoint = getRefinedGuess(a, b)
                        val acPoint = getRefinedGuess(a, c)
                        val normal = (abPoint - acPoint).cross(Vec3(0.0, -1.0, 0.0)).normalized()
                        insertRect(acPoint, abPoint, normal)
                        insertTriangle(d + upVec, b + upVec, abPoint + upVec, upDir)
                        insertTriangle(d + upVec, abPoint + upVec, acPoint + upVec, upDir)
                        insertTriangle(d + upVec, acPoint + upVec, c + upVec, upDir)
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and bType and !cType and !dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        val bdPoint = getRefinedGuess(b, d)
                        val acPoint = getRefinedGuess(a, c)
                        val normal = -(bdPoint - acPoint).cross(upDir).normalized()
                        insertRect(acPoint, bdPoint, normal)
                        meshPartBuilder.rect(
                            (acPoint + upVec).toGdxVec(),
                            (c + upVec).toGdxVec(),
                            (d + upVec).toGdxVec(),
                            (bdPoint + upVec).toGdxVec(),
                            upDir.toGdxVec()
                        )
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and dType and !bType and !cType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        val abPoint = getRefinedGuess(a, b)
                        val acPoint = getRefinedGuess(a, c)
                        val bdPoint = getRefinedGuess(b, d)
                        val cdPoint = getRefinedGuess(c, d)
                        var normal = (abPoint - cdPoint).cross(upDir).normalized()
                        insertRect(abPoint, bdPoint, normal)
                        normal = (cdPoint - abPoint).cross(upDir).normalized()
                        insertRect(cdPoint, acPoint, normal)
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and bType and cType and dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        val abPoint = getRefinedGuess(a, b)
                        val acPoint = getRefinedGuess(a, c)
                        val normal = (abPoint - acPoint).cross(upDir).normalized()
                        insertRect(abPoint, acPoint, normal)
                        insertTriangle(acPoint + upVec, abPoint + upVec, a + upVec, upDir)
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and !bType and !cType and !dType}
                        to { a: Vec3, b: Vec3, c: Vec3, d: Vec3 ->
                        meshPartBuilder.rect(
                            (a + upVec).toGdxVec(),
                            (c + upVec).toGdxVec(),
                            (d + upVec).toGdxVec(),
                            (b + upVec).toGdxVec(),
                            upDir.toGdxVec()
                        )
                    },
                )
                for (i in 0..<(samplePerSide - 1)) {
                    for (j in 0..<(samplePerSide - 1)) {
                        val a = leftBottomCorner + Vec3(i * cellSize, 0.0, j * cellSize)
                        val b = leftBottomCorner + Vec3((i + 1) * cellSize, 0.0, j * cellSize)
                        val c = leftBottomCorner + Vec3(i * cellSize, 0.0, (j + 1) * cellSize)
                        val d = leftBottomCorner + Vec3((i + 1) * cellSize, 0.0, (j + 1) * cellSize)
                        val aSample = intersectionSdf(a)
                        val bSample = intersectionSdf(b)
                        val cSample = intersectionSdf(c)
                        val dSample = intersectionSdf(d)
                        val aType = aSample > 0.0
                        val bType = bSample > 0.0
                        val cType = cSample > 0.0
                        val dType = dSample > 0.0

                        for ((match, action) in patterns) {
                            if (match(aType, bType, cType, dType)) {
                                action(a, b, c, d)
                                break
                            } else if (match(cType, aType, dType, bType)) {
                                action(c, a, d, b)
                                break
                            } else if (match(bType, dType, aType, cType)) {
                                action(b, d, a, c)
                                break
                            } else if (match(dType, cType, bType, aType)) {
                                action(d, c, b, a)
                                break
                            }
                        }
                    }
                }
                System.gc()
            }
            return modelBuilder.end()
        }
    }
}
