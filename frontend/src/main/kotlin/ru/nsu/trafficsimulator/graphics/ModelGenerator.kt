package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import ktx.math.unaryMinus
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import ru.nsu.trafficsimulator.model.Road
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

class ModelGenerator {
    companion object {
        private const val ROAD_HEIGHT = 1.0
        private const val INTERSECTION_HEIGHT = 0.98
        private const val ROAD_SEGMENT_LEN = 2.0f
        private val TO_ROAD_HEIGHT = Vec3.UP * ROAD_HEIGHT
        private val TO_INTERSECTION_HEIGHT = Vec3.UP * INTERSECTION_HEIGHT
        private const val INTERSECTION_SAMPLES_PER_SIDE = 40

        private val ROAD_MATERIAL = Material(
            RoadMaterialAttribute(),
            PBRFloatAttribute(PBRFloatAttribute.Metallic, 0.0f),
            PBRFloatAttribute(PBRFloatAttribute.Roughness, 1.0f),
        )

        fun createLayoutModel(layout: Layout): Model {
            val modelBuilder = RoadModelBuilder()
            modelBuilder.begin()
            TO_ROAD_HEIGHT.y += 0.01
            for (road in layout.roads.values) {
                addRoadToModel(road, modelBuilder)
            }
            TO_ROAD_HEIGHT.y -= 0.01

            for (intersection in layout.intersections.values) {
                addIntersectionToModel(intersection, modelBuilder)
            }
            val model = modelBuilder.end()
            return model
        }

        fun createAttributes(): VertexAttributes {
            val attributes = arrayOf(
                VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
                VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
                VertexAttribute(VertexAttributes.Usage.Generic, 1, GL20.GL_FLOAT, false, "a_heatmap"),
            )
            return VertexAttributes(*attributes)
        }

        // Function to create a color with uncapped values
        private fun colorOf(r: Float, g: Float, b: Float, a: Float): Color {
            val res = Color()
            res.r = r
            res.g = g
            res.b = b
            res.a = a
            return res
        }

        // Unpacked color in models below is used to convey information about lanes
        // And is used to create road markings
        private fun addRoadToModel(road: Road, modelBuilder: ModelBuilder) {
            val node = modelBuilder.node()
            node.translation.set(0.0f, 0.0f, 0.0f)
            val meshPartBuilder = modelBuilder.part(
                "road${road.id}",
                GL20.GL_TRIANGLES,
                createAttributes(),
                ROAD_MATERIAL
            )

            val hasStart = road.startIntersection.intersectionRoads.isNotEmpty()
            val hasEnd = road.endIntersection.intersectionRoads.isNotEmpty()
            val length = road.geometry.length - if (hasStart) { road.startIntersection.padding } else { 0.0 } - if (hasEnd) { road.endIntersection.padding } else { 0.0 }

            val start = if (hasStart) { road.startIntersection.padding } else { 0.0 }
            val rightLaneCntF = road.rightLane.toFloat()
            val leftLaneCntF = -road.leftLane.toFloat()

            val stepCount = floor(length / ROAD_SEGMENT_LEN).toInt()
            var prevPos = road.geometry.getPoint(start).toVec3()
            var prevDir = road.geometry.getDirection(start).toVec3().normalized()
            var prevRight = prevDir.cross(Vec3.UP).normalized() * LANE_WIDTH
            var prevLeft = -prevRight
            meshPartBuilder.rect(
                MeshPartBuilder.VertexInfo().set((prevPos + prevLeft).toGdxVec(), -prevDir.toGdxVec(), Color.CLEAR, null),
                MeshPartBuilder.VertexInfo().set((prevPos + prevRight).toGdxVec(), -prevDir.toGdxVec(), Color.CLEAR, null),
                MeshPartBuilder.VertexInfo().set((prevPos + prevRight + TO_ROAD_HEIGHT).toGdxVec(), -prevDir.toGdxVec(), Color.CLEAR, null),
                MeshPartBuilder.VertexInfo().set((prevPos + prevLeft + TO_ROAD_HEIGHT).toGdxVec(), -prevDir.toGdxVec(), Color.CLEAR, null),
            )

            val insertSegment = fun(left: Vec3, pos: Vec3, right: Vec3, prevOffset: Double, offset: Double) {
                // Top
                for (lane in -road.leftLane until road.rightLane) {
                    val startLane = lane.toDouble()
                    val endLane = (lane + 1).toDouble()
                    meshPartBuilder.rect(
                        MeshPartBuilder.VertexInfo().set(
                            (prevPos + prevRight * endLane + TO_ROAD_HEIGHT).toGdxVec(),
                            Vec3.UP.toGdxVec(),
                            colorOf(endLane.toFloat(), prevOffset.toFloat(), leftLaneCntF, rightLaneCntF),
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            (pos + right * endLane + TO_ROAD_HEIGHT).toGdxVec(),
                            Vec3.UP.toGdxVec(),
                            colorOf(endLane.toFloat(), offset.toFloat(), leftLaneCntF, rightLaneCntF),
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            (pos + right * startLane + TO_ROAD_HEIGHT).toGdxVec(),
                            Vec3.UP.toGdxVec(),
                            colorOf(startLane.toFloat(), offset.toFloat(), leftLaneCntF, rightLaneCntF),
                            null
                        ),
                        MeshPartBuilder.VertexInfo().set(
                            (prevPos + prevRight * startLane + TO_ROAD_HEIGHT).toGdxVec(),
                            Vec3.UP.toGdxVec(),
                            colorOf(startLane.toFloat(), prevOffset.toFloat(), leftLaneCntF, rightLaneCntF),
                            null
                        ),
                    )
                }

                // Right
                val rightNormal = (pos - prevPos).cross(Vec3.UP).toGdxVec()
                meshPartBuilder.rect(
                    MeshPartBuilder.VertexInfo().set((prevPos + prevRight).toGdxVec(), rightNormal, Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((pos + right).toGdxVec(), rightNormal, Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((pos + right + TO_ROAD_HEIGHT).toGdxVec(), rightNormal, Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((prevPos + prevRight + TO_ROAD_HEIGHT).toGdxVec(), rightNormal, Color.CLEAR, null),
                )

                // Left
                val leftNormal = -rightNormal
                meshPartBuilder.rect(
                    MeshPartBuilder.VertexInfo().set((pos + left).toGdxVec(), leftNormal, Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((prevPos + prevLeft).toGdxVec(), leftNormal, Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((prevPos + prevLeft + TO_ROAD_HEIGHT).toGdxVec(), leftNormal, Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((pos + left + TO_ROAD_HEIGHT).toGdxVec(), leftNormal, Color.CLEAR, null),
                )
            }

            for (i in 1..stepCount) {
                val t = start + i * ROAD_SEGMENT_LEN.toDouble()
                val pos = road.geometry.getPoint(t).toVec3()
                val direction = road.geometry.getDirection(t).toVec3().normalized()
                val right = direction.cross(Vec3.UP).normalized() * LANE_WIDTH
                val left = -right
                if (direction.dot(prevDir) < 0.0) {
                    prevLeft = prevRight.also { prevRight = prevLeft }
                }

                insertSegment(left, pos, right, (i - 1) * ROAD_SEGMENT_LEN.toDouble(), i * ROAD_SEGMENT_LEN.toDouble())

                prevPos = pos
                prevLeft = left
                prevRight = right
                prevDir = direction
            }
            val t = road.geometry.length - if (hasEnd) { road.endIntersection.padding } else { 0.0 }
            val pos = road.geometry.getPoint(t).toVec3()
            val direction = road.geometry.getDirection(t).toVec3().normalized()
            val right = direction.cross(Vec3.UP).normalized() * LANE_WIDTH
            val left = -direction.cross(Vec3.UP).normalized() * LANE_WIDTH
            insertSegment(left, pos, right, start + stepCount * ROAD_SEGMENT_LEN.toDouble(), t)

            val endNormal = direction.toGdxVec()
            meshPartBuilder.rect(
                MeshPartBuilder.VertexInfo().set((pos + right).toGdxVec(), endNormal, Color.CLEAR, null),
                MeshPartBuilder.VertexInfo().set((pos + left).toGdxVec(), endNormal, Color.CLEAR, null),
                MeshPartBuilder.VertexInfo().set((pos + left + TO_ROAD_HEIGHT).toGdxVec(), endNormal, Color.CLEAR, null),
                MeshPartBuilder.VertexInfo().set((pos + right + TO_ROAD_HEIGHT).toGdxVec(), endNormal, Color.CLEAR, null),
            )
        }

        private fun addIntersectionToModel(intersection: Intersection, modelBuilder: ModelBuilder) {
            if (intersection.intersectionRoads.isEmpty()) {
                return
            }

            val intersectionBoxSize = max(intersection.padding * 2.0 * 1.1, 40.0)
            val cellSize = intersectionBoxSize / (INTERSECTION_SAMPLES_PER_SIDE - 1).toDouble()

            val node = modelBuilder.node()
            node.translation.set(intersection.position.toVec3().toGdxVec())
            val meshPartBuilder = modelBuilder.part(
                "intersection${intersection.id}",
                GL20.GL_TRIANGLES,
                createAttributes(),
                ROAD_MATERIAL
            )
            val intersectionSdf = { local: Vec2 ->
                val point = intersection.position + local
                var minDist = intersectionBoxSize * intersectionBoxSize
                var laneCount = 1
                for ((_, road) in intersection.intersectionRoads) {
                    val dist = (road.geometry.closestPoint(point).first - point).length()
                    if (abs(dist) < abs(minDist)) {
                        minDist = dist
                        laneCount = 1
                    }
                }
                minDist - LANE_WIDTH * laneCount
            }
            val insertRect = { a: Vec3, b: Vec3, normal: Vec3 ->
                meshPartBuilder.rect(
                    MeshPartBuilder.VertexInfo().set((a + TO_INTERSECTION_HEIGHT).toGdxVec(), normal.toGdxVec(), Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set(a.toGdxVec(), normal.toGdxVec(), Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set(b.toGdxVec(), normal.toGdxVec(), Color.CLEAR, null),
                    MeshPartBuilder.VertexInfo().set((b + TO_INTERSECTION_HEIGHT).toGdxVec(), normal.toGdxVec(), Color.CLEAR, null),
                )
            }
            val insertTriangle = { a: Vec3, b: Vec3, c: Vec3, normal: Vec3 ->
                meshPartBuilder.triangle(
                    MeshPartBuilder.VertexInfo().set(
                        a.toGdxVec(),
                        normal.toGdxVec(),
                        Color.CLEAR,
                        null
                    ),
                    MeshPartBuilder.VertexInfo().set(
                        b.toGdxVec(),
                        normal.toGdxVec(),
                        Color.CLEAR,
                        null
                    ),
                    MeshPartBuilder.VertexInfo().set(
                        c.toGdxVec(),
                        normal.toGdxVec(),
                        Color.CLEAR,
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
                    insertTriangle(d.toVec3() + TO_INTERSECTION_HEIGHT, abPoint + TO_INTERSECTION_HEIGHT, b.toVec3() + TO_INTERSECTION_HEIGHT, Vec3.UP)
                    insertTriangle(d.toVec3() + TO_INTERSECTION_HEIGHT, acPoint + TO_INTERSECTION_HEIGHT, abPoint + TO_INTERSECTION_HEIGHT, Vec3.UP)
                    insertTriangle(d.toVec3() + TO_INTERSECTION_HEIGHT, c.toVec3() + TO_INTERSECTION_HEIGHT, acPoint + TO_INTERSECTION_HEIGHT, Vec3.UP)
                },
                { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> aType and bType and !cType and !dType }
                    to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                    val bdPoint = getRefinedGuess(b, d).toVec3()
                    val acPoint = getRefinedGuess(a, c).toVec3()
                    val normal = (bdPoint - acPoint).cross(Vec3.UP).normalized()
                    insertRect(acPoint, bdPoint, normal)
                    meshPartBuilder.rect(
                        MeshPartBuilder.VertexInfo().set((c.toVec3() + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                        MeshPartBuilder.VertexInfo().set((acPoint + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                        MeshPartBuilder.VertexInfo().set((bdPoint + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                        MeshPartBuilder.VertexInfo().set((d.toVec3() + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
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
                    insertTriangle(acPoint + TO_INTERSECTION_HEIGHT, a.toVec3() + TO_INTERSECTION_HEIGHT, abPoint + TO_INTERSECTION_HEIGHT, Vec3.UP)
                },
                { aType: Boolean, bType: Boolean, cType: Boolean, dType: Boolean -> !aType and !bType and !cType and !dType }
                    to { a: Vec2, b: Vec2, c: Vec2, d: Vec2 ->
                    meshPartBuilder.rect(
                        MeshPartBuilder.VertexInfo().set((a.toVec3() + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                        MeshPartBuilder.VertexInfo().set((b.toVec3() + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                        MeshPartBuilder.VertexInfo().set((d.toVec3() + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                        MeshPartBuilder.VertexInfo().set((c.toVec3() + TO_INTERSECTION_HEIGHT).toGdxVec(), Vec3.UP.toGdxVec(), Color.CLEAR, null),
                    )
                },
            )
            for (i in 0..<(INTERSECTION_SAMPLES_PER_SIDE - 1)) {
                for (j in 0..<(INTERSECTION_SAMPLES_PER_SIDE - 1)) {
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
    }
}
