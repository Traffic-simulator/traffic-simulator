package ru.nsu.trafficsimulator.math

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

private const val roadIntersectionThreshold: Double = 5.0

fun getIntersectionWithGround(screenPos: Vec2, camera: Camera): Vector3? {
    val ray = camera.getPickRay(screenPos.x.toFloat(), screenPos.y.toFloat())
    val intersection = Vector3()
    val plane = Plane(Vector3(0f, 1f, 0f), 0f)
    if (Intersector.intersectRayPlane(ray, plane, intersection)) {
        return intersection
    }
    return null
}

fun findRoad(layout: Layout, point: Vector3): Road? {
    var minDistance = Double.MAX_VALUE
    var closestRoad : Road? = null
    val point2d = Vec2(point.x.toDouble(), point.z.toDouble())
    for (road in layout.roads.values) {
        val distance = road.geometry.closestPoint(point2d).distance(point2d)
        if (distance < minDistance) {
            minDistance = distance
            closestRoad = road
        }
    }
    if (minDistance < roadIntersectionThreshold) {
        return closestRoad
    } else {
        return null
    }
}
