import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class AddRoadStateChangeDetailedTest {

    @Test
    fun `add road between two intersections - basic test`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(20.0, 10.0), 0.0)
        val endPos = Intersection(2, Vec2(-20.0, -10.0), 0.0)

        val change = AddRoadStateChange(startPos, Vec3(-2.0, 0.0, 0.0), endPos, Vec3(2.0, 0.0, 0.0))

        change.apply(layout)
        change.apply(layout)

        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertTrue(layout.intersectionRoads.isEmpty())

        change.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, layout.intersections.size)
        assertTrue(layout.intersectionRoads.isEmpty())
    }

    @Test
    fun `add road should create valid road object`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val change = AddRoadStateChange(
            startPos,
            Vec3(1.0, 0.0, 0.0),
            endPos,
            Vec3(-1.0, 0.0, 0.0)
        )

        change.apply(layout)

        val road = layout.roads.values.first()
        assertNotNull(road)
        assertEquals(startPos, road.startIntersection)
        assertEquals(endPos, road.endIntersection)
        assertTrue(road.length > 0)
        assertEquals(0, layout.intersectionRoads.size)

        change.revert(layout)
    }

    @Test
    fun `add road should maintain intersections collection`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val change = AddRoadStateChange(
            startPos,
            Vec3(1.0, 0.0, 0.0),
            endPos,
            Vec3(-1.0, 0.0, 0.0)
        )

        assertEquals(0, layout.intersections.size)

        change.apply(layout)

        assertEquals(2, layout.intersections.size)
        assertTrue(layout.intersections.containsKey(1))
        assertTrue(layout.intersections.containsKey(2))
        assertEquals(startPos, layout.intersections[1])
        assertEquals(endPos, layout.intersections[2])

        change.revert(layout)

        assertEquals(0, layout.intersections.size)
    }

    @Test
    fun `add road should create proper connections`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val change = AddRoadStateChange(
            startPos,
            Vec3(1.0, 0.0, 0.0),
            endPos,
            Vec3(-1.0, 0.0, 0.0)
        )

        change.apply(layout)

        val road = layout.roads.values.first()

        assertEquals(1, startPos.incomingRoads.size, "У стартового перекрёстка должна быть одна дорога")
        assertTrue(startPos.incomingRoads.contains(road), "Дорога должна быть в списке incomingRoads")
        assertTrue(startPos.intersectionRoads.isEmpty(), "Должны быть созданы intersectionRoads")

        assertEquals(1, endPos.incomingRoads.size, "У конечного перекрёстка должна быть одна дорога")
        assertTrue(endPos.incomingRoads.contains(road), "Дорога должна быть в списке incomingRoads")
        assertTrue(endPos.intersectionRoads.isEmpty(), "Должны быть созданы intersectionRoads")

        change.revert(layout)
    }

    @Test
    fun `multiple add and undo operations`() {
        val layout = Layout()
        val pos1 = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val pos2 = Intersection(2, Vec2(10.0, 0.0), 0.0)
        val pos3 = Intersection(3, Vec2(20.0, 0.0), 0.0)

        val change1 = AddRoadStateChange(
            pos1,
            Vec3(1.0, 0.0, 0.0),
            pos2,
            Vec3(-1.0, 0.0, 0.0)
        )
        change1.apply(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(0, layout.intersectionRoads.size)

        val change2 = AddRoadStateChange(
            pos2,
            Vec3(1.0, 0.0, 0.0),
            pos3,
            Vec3(-1.0, 0.0, 0.0)
        )
        change2.apply(layout)
        assertEquals(2, layout.roads.size)
        assertEquals(2, layout.intersectionRoads.size)

        change2.revert(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(0, layout.intersectionRoads.size)

        change1.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)
    }

    @Test
    fun `create and undo triangular road cycle`() {
        val layout = Layout()

        val pos1 = Vec2(0.0, 0.0)
        val pos2 = Vec2(10.0, 0.0)
        val pos3 = Vec2(10.0, 10.0)

        val intersection1 = Intersection(1, pos1, 0.0)
        val intersection2 = Intersection(2, pos2, 0.0)
        val intersection3 = Intersection(3, pos3, 0.0)

        val change1 = AddRoadStateChange(
            intersection1,
            Vec3(1.0, 0.0, 0.0),
            intersection2,
            Vec3(-1.0, 0.0, 0.0),
        )

        val change2 = AddRoadStateChange(
            intersection2,
            Vec3(1.0, 0.0, 0.0),
            intersection3,
            Vec3(-1.0, 0.0, 0.0),
        )

        val change3 = AddRoadStateChange(
            intersection3,
            Vec3(1.0, 0.0, 0.0),
            intersection1,
            Vec3(-1.0, 0.0, 0.0),
        )

        change1.apply(layout)
        change2.apply(layout)
        change3.apply(layout)

        assertEquals(3, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(6, layout.intersectionRoads.size)

        change3.revert(layout)
        assertEquals(2, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(2, layout.intersectionRoads.size)

        change2.revert(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)

        change1.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, intersection1.intersectionRoads.size)
        assertEquals(0, intersection2.intersectionRoads.size)
        assertEquals(0, intersection3.intersectionRoads.size)
    }
}
