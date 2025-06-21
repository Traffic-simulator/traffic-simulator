package ru.nsu.trafficsimulator.math

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.OrientedBoundingBox
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Layout.Companion.LANE_WIDTH
import ru.nsu.trafficsimulator.model.Road
import ru.nsu.trafficsimulator.model.Vehicle
import ru.nsu.trafficsimulator.serializer.Deserializer
import vehicle.Direction
import java.lang.Math.clamp
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.sign

fun getIntersectionWithGround(screenPos: Vec2, camera: Camera): Vec3? {
    val ray = camera.getPickRay(screenPos.x.toFloat(), screenPos.y.toFloat())
    val intersection = Vector3()
    val plane = Plane(Vector3(0f, 1f, 0f), 0f)
    if (Intersector.intersectRayPlane(ray, plane, intersection)) {
        return Vec3(intersection)
    }
    return null
}

fun findIntersectionWithCar(vehicles: List<Vehicle>, screenPos: Vec2, camera: Camera): Vehicle? {
    val ray = camera.getPickRay(screenPos.x.toFloat(), screenPos.y.toFloat())
    for (vehicle in vehicles) {
        val intersection = Vector3()
        val boundingBox = BoundingBox(Vector3(-3.3f, 0.0f, -1.36f), Vector3(2.63f, 2.62f, 1.35f))
        val orientedBoundingBox = OrientedBoundingBox(boundingBox, vehicle.transform)
        if (Intersector.intersectRayOrientedBounds(ray, orientedBoundingBox, intersection)) {
            return vehicle
        }
    }
    return null
}

fun transformVehicles(vehicles: List<ISimulation.VehicleDTO>): List<Vehicle> {
    return vehicles.map { vehicle ->
        val carRoad = vehicle.road
        val spline = Deserializer.planeViewToSpline(carRoad.planView)
        val pointOnSpline = clamp(
            if (vehicle.direction == Direction.BACKWARD) {
                spline.length - vehicle.distance
            } else {
                vehicle.distance
            }, 0.0, spline.length
        )
        val pos = spline.getPoint(pointOnSpline)
        val dir = spline.getDirection(pointOnSpline).normalized() * if (vehicle.direction == Direction.BACKWARD) {
            -1.0
        } else {
            1.0
        }
        val right = dir.toVec3().cross(Vec3.UP).normalized()
        val angle = acos(dir.x) * sign(dir.y) + getVehicleLaneChangeAngle(vehicle)
        val laneOffset = getVehicleOffset(vehicle)
        val finalTranslation = pos.toVec3() + right * laneOffset * LANE_WIDTH + Vec3.UP
        val transform = Matrix4()
            .setToRotationRad(Vec3.UP.toGdxVec(), angle.toFloat())
            .setTranslation(finalTranslation.toGdxVec())
        Vehicle(vehicle.id, transform)
    }
}

private fun getVehicleOffset(vehicle: ISimulation.VehicleDTO): Double {
    if (vehicle.laneChangeInfo == null) {
        return abs(vehicle.laneId) - 0.5
    }

    val lcInfo = vehicle.laneChangeInfo!!
    val base = abs(lcInfo.toLaneId) - 0.5
    val addition = 1.0 - customSigmoid(lcInfo.laneChangeCurrentDistance, lcInfo.laneChangeFullDistance, )
    if (abs(lcInfo.toLaneId) < abs(lcInfo.fromLaneId)) {
        return base + addition
    }
    return base - addition
}


private val middlePartLaneChangeAngle = customSigmoid(2.0, 6.0)
private fun getVehicleLaneChangeAngle(vehicle: ISimulation.VehicleDTO): Double {
    if (vehicle.laneChangeInfo == null) {
        return 0.0
    }

    val lcInfo = vehicle.laneChangeInfo!!
    val angle: Double
    if (lcInfo.laneChangeCurrentDistance < lcInfo.laneChangeFullDistance / 3.0) {
        angle = customSigmoid(lcInfo.laneChangeCurrentDistance, lcInfo.laneChangeFullDistance)
    } else
        if (lcInfo.laneChangeCurrentDistance < 2.0 * lcInfo.laneChangeFullDistance / 3.0){
            angle = middlePartLaneChangeAngle
        } else {
            angle = (1.0 - customSigmoid(lcInfo.laneChangeCurrentDistance, lcInfo.laneChangeFullDistance))
        }

    if (abs(lcInfo.toLaneId) < abs(lcInfo.fromLaneId)) {
        return angle
    }
    return -angle
}

inline fun <T> findNearestObject(vec3: Vec3, list: List<T>, threshold: Double = Double.MAX_VALUE, toVec3: (T) -> Vec3): T? {
    list.minByOrNull { toVec3(it).distance(vec3) }?.let {
        return if (toVec3(it).distance(vec3) < threshold) it else null
    }
    return null
}

inline fun <T> isNear(vec3: Vec3, obj: T, threshold: Double, toVec3: (T) -> Vec3) =
    toVec3(obj).distance(vec3) <= threshold


fun findRoad(layout: Layout, point: Vec3): Road? {
    val point2d = point.xzProjection()
    for (road in layout.roads.values) {
        val (closestPoint, direction) = road.geometry.closestPoint(point2d)
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

fun customSigmoid(x: Double, t: Double, steepness: Double = 8.0, midpointRatio: Double = 0.5): Double {
    /**
     * Sigmoid-like function that goes from (0,0) to (t,1)
     *
     * @param x Input value
     * @param t Endpoint t-coordinate where f(t) = 1
     * @param steepness Controls how steep the transition is (default: 10)
     * @param midpointRatio Where the midpoint occurs as fraction of x (default: 0.5)
     * @return Value between 0 and 1
     */
    val midpoint = t * midpointRatio
    val transformedT = steepness * (x - midpoint) / t

    // Compute modified sigmoid
    val sig = 1.0 / (1.0 + exp(-transformedT))

    // Normalize to ensure f(0)=0 and f(t)=1
    val sig0 = 1.0 / (1.0 + exp(steepness * midpointRatio))
    val sigX = 1.0 / (1.0 + exp(-steepness * (1.0 - midpointRatio)))

    return (sig - sig0) / (sigX - sig0)
}
