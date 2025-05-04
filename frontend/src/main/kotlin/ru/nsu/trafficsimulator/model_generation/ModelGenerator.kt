package ru.nsu.trafficsimulator.model_generation

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import ktx.math.unaryMinus
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class ModelGenerator {
    companion object {
        private val roadHeight = 1.0
        private val splineRoadSegmentLen = 2.0f
        private val upVec = Vec3.UP * roadHeight

        fun createLayoutModel(layout: Layout): Model {
            val modelBuilder = ModelBuilder()
            var meshPartBuilder: MeshPartBuilder
            modelBuilder.begin()
            for (road in layout.roads.values) {
                val node = modelBuilder.node()
                node.translation.set(0.0f, 0.0f, 0.0f)
                meshPartBuilder = modelBuilder.part("road${road.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())


                val hasStart = road.startIntersection.intersectionRoads.size > 0
                val hasEnd = road.endIntersection.intersectionRoads.size > 0
                val length = road.geometry.length - if (hasStart) { road.startIntersection.padding } else { 0.0 } - if (hasEnd) { road.endIntersection.padding } else { 0.0 }

                val start = if (hasStart) { road.startIntersection.padding } else { 0.0 }

                val stepCount = floor(length / splineRoadSegmentLen).toInt()
                var prevPos = road.geometry.getPoint(start).toVec3()
                var prevDir = road.geometry.getDirection(start).toVec3().normalized()
                var prevRight = prevDir.cross(Vec3.UP).normalized() * LANE_WIDTH * road.rightLane.toDouble()
                var prevLeft = -prevDir.cross(Vec3.UP).normalized() * LANE_WIDTH * road.leftLane.toDouble()
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
                    val right = direction.cross(Vec3.UP).normalized() * LANE_WIDTH * road.rightLane.toDouble()
                    val left = -direction.cross(Vec3.UP).normalized() * LANE_WIDTH * road.leftLane.toDouble()
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
                val t = road.geometry.length - if (hasEnd) { road.endIntersection.padding } else { 0.0 }
                val pos = road.geometry.getPoint(t).toVec3()
                val direction = road.geometry.getDirection(t).toVec3().normalized()
                val right = direction.cross(Vec3.UP).normalized() * LANE_WIDTH * road.rightLane.toDouble()
                val left = -direction.cross(Vec3.UP).normalized() * LANE_WIDTH * road.leftLane.toDouble()
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
            val samplePerSide = 40
            for (intersection in layout.intersections.values) {
                val intersectionBoxSize = max(intersection.padding * 2.0 * 1.1, 40.0)
                val cellSize = intersectionBoxSize / (samplePerSide - 1).toDouble()

                val node = modelBuilder.node()
                node.translation.set(intersection.position.toVec3().toGdxVec())
                meshPartBuilder = modelBuilder.part("intersection${intersection.id}", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), Material())
                val intersectionSdf = { local: Vec2 ->
                    val point = intersection.position + local
                    var minDist = intersectionBoxSize * intersectionBoxSize
                    var laneCount = 1
                    for (road in intersection.intersectionRoads) {
                        val dist = (road.geometry.closestPoint(point) - point).length()
                        if (abs(dist) < abs(minDist)) {
                            minDist = dist
                            laneCount = 1
                        }
                    }
                    minDist - LANE_WIDTH * laneCount
                }
                val insertRect = { a: Vec3, b: Vec3, normal: Vec3 ->
                    meshPartBuilder.rect(
                        (a + upVec).toGdxVec(),
                        a.toGdxVec(),
                        b.toGdxVec(),
                        (b + upVec).toGdxVec(),
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
                val getRefinedGuess = { a: Vec2, b: Vec2 ->
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
                val leftBottomCorner = Vec2(-intersectionBoxSize / 2.0, -intersectionBoxSize / 2.0)
                val patterns = arrayOf(
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and !bType and !cType and !dType}
                        to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                        val abPoint = getRefinedGuess(a, b).toVec3()
                        val acPoint = getRefinedGuess(a, c).toVec3()
                        val normal = (abPoint - acPoint).cross(Vec3.UP).normalized()
                        insertRect(acPoint, abPoint, normal)
                        insertTriangle(d.toVec3() + upVec, abPoint + upVec, b.toVec3() + upVec, Vec3.UP)
                        insertTriangle(d.toVec3() + upVec, acPoint + upVec, abPoint + upVec, Vec3.UP)
                        insertTriangle(d.toVec3() + upVec, c.toVec3() + upVec, acPoint + upVec, Vec3.UP)
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and bType and !cType and !dType}
                        to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                        val bdPoint = getRefinedGuess(b, d).toVec3()
                        val acPoint = getRefinedGuess(a, c).toVec3()
                        val normal = (bdPoint - acPoint).cross(Vec3.UP).normalized()
                        insertRect(acPoint, bdPoint, normal)
                        meshPartBuilder.rect(
                            (c.toVec3() + upVec).toGdxVec(),
                            (acPoint + upVec).toGdxVec(),
                            (bdPoint + upVec).toGdxVec(),
                            (d.toVec3() + upVec).toGdxVec(),
                            Vec3.UP.toGdxVec()
                        )
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and dType and !bType and !cType}
                        to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                        val abPoint = getRefinedGuess(a, b).toVec3()
                        val acPoint = getRefinedGuess(a, c).toVec3()
                        val bdPoint = getRefinedGuess(b, d).toVec3()
                        val cdPoint = getRefinedGuess(c, d).toVec3()
                        var normal = (cdPoint - abPoint).cross(Vec3.UP).normalized()
                        insertRect(abPoint, bdPoint, normal)
                        normal = (abPoint - cdPoint).cross(Vec3.UP).normalized()
                        insertRect(cdPoint, acPoint, normal)
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and bType and cType and dType}
                        to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                        val abPoint = getRefinedGuess(a, b).toVec3()
                        val acPoint = getRefinedGuess(a, c).toVec3()
                        val normal = (acPoint - abPoint).cross(Vec3.UP).normalized()
                        insertRect(abPoint, acPoint, normal)
                        insertTriangle(acPoint + upVec, a.toVec3() + upVec, abPoint + upVec, Vec3.UP)
                    },
                    {aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and !bType and !cType and !dType}
                        to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                        meshPartBuilder.rect(
                            (a.toVec3() + upVec).toGdxVec(),
                            (b.toVec3() + upVec).toGdxVec(),
                            (d.toVec3() + upVec).toGdxVec(),
                            (c.toVec3() + upVec).toGdxVec(),
                            Vec3.UP.toGdxVec()
                        )
                    },
                )
                for (i in 0..<(samplePerSide - 1)) {
                    for (j in 0..<(samplePerSide - 1)) {
                        val a = leftBottomCorner + Vec2(i, j) * cellSize
                        val b = leftBottomCorner + Vec2(i + 1, j) * cellSize
                        val c = leftBottomCorner + Vec2(i, j + 1) * cellSize
                        val d = leftBottomCorner + Vec2(i + 1, j + 1) * cellSize
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
