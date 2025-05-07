import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import ru.nsu.trafficsimulator.editor.changes.RedirectRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class RedirectRoadStateChangeTest {

    @Test
    fun `should redirect road and revert correctly`() {
        val layout = Layout()
        val startIntersection = Intersection(1, Vec2(0.0, 0.0))
        val endIntersection = Intersection(2, Vec2(10.0, 0.0))

        val road = layout.addRoad(
            startIntersection,
            startIntersection.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            endIntersection,
            endIntersection.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val initialStartDir = road.getDirection(0.0)
        val initialEndDir = road.getDirection(road.length)

        val newStartDir = Vec3(1.0, 1.0, 0.0)
        val newEndDir = Vec3(-1.0, -1.0, 0.0)
        val change = RedirectRoadStateChange(road, newStartDir, newEndDir)

        change.apply(layout)
        change.revert(layout)
        assertEquals(initialStartDir, road.getDirection(0.0))
        assertEquals(initialEndDir, road.getDirection(road.length))
    }

    @Test
    fun `multiple redirect and revert`() {
        val layout = Layout()
        val startIntersection = Intersection(1, Vec2(0.0, 0.0))
        val endIntersection = Intersection(2, Vec2(10.0, 0.0))
        val end2Intersection = Intersection(3, Vec2(20.0, 0.0))


        val road = layout.addRoad(
            startIntersection,
            startIntersection.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            endIntersection,
            endIntersection.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val road2 = layout.addRoad(
            startIntersection,
            startIntersection.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            end2Intersection,
            endIntersection.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val initialStartDir = road.getDirection(0.0)
        val initialEndDir = road.getDirection(road.length)

        val newStartDir = Vec3(1.0, 1.0, 0.0)
        val newEndDir = Vec3(-1.0, -1.0, 0.0)
        val change = RedirectRoadStateChange(road, newStartDir, newEndDir)
        val change2 = RedirectRoadStateChange(road, newStartDir, newEndDir)

        change.apply(layout)
        change.revert(layout)
        change2.apply(layout)
        change2.revert(layout)
        assertEquals(initialStartDir, road.getDirection(0.0))
        assertEquals(initialEndDir, road.getDirection(road.length))
    }
}
