package ru.nsu.trafficsimulator.math

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import ru.nsu.trafficsimulator.model.Road

fun getIntersectionWithGround(screenPos: Vec2, camera: Camera): Vec3? {
    val ray = camera.getPickRay(screenPos.x.toFloat(), screenPos.y.toFloat())
    val intersection = Vector3()
    val plane = Plane(Vector3(0f, 1f, 0f), 0f)
    if (Intersector.intersectRayPlane(ray, plane, intersection)) {
        return Vec3(intersection)
    }
    return null
}

fun <T> findNearestObject(vec3: Vec3, list: List<T>, threshold: Double = Double.MAX_VALUE, toVec3: (T) -> Vec3): T? {
    list.minByOrNull { toVec3(it).distance(vec3) }?.let {
        return if (toVec3(it).distance(vec3) < threshold) it else null
    }
    return null
}

fun findRoad(layout: Layout, point: Vec3): Road? {
    val point2d = point.xzProjection()
    for (road in layout.roads.values) {
        val (closestPoint, pointOffset) = road.geometry.closestPoint(point2d)
        val direction = road.geometry.getDirection(pointOffset).normalized()
        val toRight = direction.toVec3().cross(Vec3.UP)
        val laneCount = if ((point - closestPoint.toVec3()).dot(toRight) > 0.0) {
            road.rightLane
        } else {
            road.leftLane
        }
        if ((closestPoint - point2d).length() <= laneCount * LANE_WIDTH) {
            return road
        }
    }
    return null
}

fun findRoadIntersectionAt(layout: Layout, point: Vec3): Intersection? {
    for ((_, intersection) in layout.intersections) {
        if (intersection.position.distance(point.xzProjection()) < 5.0f) {
            return intersection
        }
    }
    return null
}

