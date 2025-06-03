import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import ru.nsu.trafficsimulator.editor.changes.MoveIntersectionStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class MoveIntersectionStateChangeTest {

    @Test
    fun `should move intersection and revert correctly`() {
        val layout = Layout()
        val initialPos = Vec2(0.0, 0.0)
        val intersection = Intersection(1, initialPos)
        layout.intersections[intersection.id] = intersection

        val newPos = Vec3(10.0, 0.0, 0.0)
        val change = MoveIntersectionStateChange(intersection, newPos)

        change.apply(layout)
        assertEquals(newPos.xzProjection(), intersection.position,
            "Позиция перекрёстка должна измениться")

        change.revert(layout)
        assertEquals(initialPos, intersection.position,
            "Позиция должна вернуться к исходной")
    }

    @Test
    fun `should handle multiple move operations`() {
        val layout = Layout()
        val intersection = Intersection(1, Vec2(0.0, 0.0))
        layout.intersections[intersection.id] = intersection

        val move1 = MoveIntersectionStateChange(intersection, Vec3(5.0, 0.0, 0.0))
        move1.apply(layout)
        assertEquals(Vec2(5.0, -0.0), intersection.position)

        val move2 = MoveIntersectionStateChange(intersection, Vec3(10.0, 0.0, 0.0))
        move2.apply(layout)
        assertEquals(Vec2(10.0, -0.0), intersection.position)

        move2.revert(layout)
        assertEquals(Vec2(5.0, -0.0), intersection.position)

        move1.revert(layout)
        assertEquals(Vec2(0.0, 0.0), intersection.position)
    }

    @Test
    fun `should maintain connections when moving`() {
        val layout = Layout()
        val intersection = Intersection(1, Vec2(0.0, 0.0))
        layout.intersections[intersection.id] = intersection

        val otherIntersection = Intersection(2, Vec2(50.0, 0.0))
        layout.intersections[otherIntersection.id] = otherIntersection
        val road = layout.addRoad(
            intersection,
            intersection.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            otherIntersection,
            otherIntersection.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val initialConnections = intersection.intersectionRoads.size
        val newPos = Vec3(30.0, 0.0, 0.0)
        val change = MoveIntersectionStateChange(intersection, newPos)

        change.apply(layout)
        assertEquals(initialConnections, intersection.intersectionRoads.size,
            "Количество соединений не должно измениться")

        change.revert(layout)
        assertEquals(initialConnections, intersection.intersectionRoads.size,
            "Количество соединений должно остаться прежним")
    }
}
