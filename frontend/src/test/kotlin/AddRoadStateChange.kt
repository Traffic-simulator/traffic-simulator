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

        val change = AddRoadStateChange(
            Vec3(20.0, 10.0, 0.0) to Vec3(-2.0, 0.0, 0.0), null,
            Vec3(-20.0, -10.0, 0.0) to Vec3(2.0, 0.0, 0.0), null
        )

        change.apply(layout)
        change.apply(layout)

        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertTrue(layout.intersectionRoadsNumber == 0)

        change.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, layout.intersections.size)
        assertTrue(layout.intersectionRoadsNumber == 0)
    }

    @Test
    fun `add road should create valid road object`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val change = AddRoadStateChange(
            Vec3(0.0, 0.0, 0.0) to Vec3(1.0, 0.0, 0.0),
            startPos,
            Vec3(10.0, 0.0, 0.0) to Vec3(-1.0, 0.0, 0.0),
            endPos
        )

        change.apply(layout)

        val road = layout.roads.values.first()
        assertNotNull(road)
        assertEquals(startPos, road.startIntersection)
        assertEquals(endPos, road.endIntersection)
        assertTrue(road.length > 0)
        assertEquals(0, layout.intersectionRoadsNumber)

        change.revert(layout)
    }

    @Test
    fun `add road should maintain intersections collection`() {
        val layout = Layout()

        val change = AddRoadStateChange(
            Vec3(0.0, 0.0, 0.0) to Vec3(1.0, 0.0, 0.0),
            null,
            Vec3(30.0, 0.0, 0.0) to Vec3(-1.0, 0.0, 0.0),
            null
        )

        assertEquals(0, layout.intersections.size)

        change.apply(layout)

        assertEquals(2, layout.intersections.size)

        change.revert(layout)

        assertEquals(0, layout.intersections.size)
    }

    @Test
    fun `add road should create proper connections`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val change = AddRoadStateChange(
            Vec3(0.0, 0.0, 0.0) to Vec3(1.0, 0.0, 0.0),
            startPos,
            Vec3(10.0, 0.0, 0.0) to Vec3(-1.0, 0.0, 0.0),
            endPos
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

        val change1 = AddRoadStateChange(
            Vec3(0.0, 0.0, 0.0) to Vec3(1.0, 0.0, 0.0),
            null,
            Vec3(30.0, 0.0, 0.0) to Vec3(-1.0, 0.0, 0.0),
            null
        )

        change1.apply(layout)
        val pos2 = layout.intersections[1] ?: throw IllegalArgumentException("sosi")

        assertEquals(1, layout.roads.size)
        assertEquals(0, layout.intersectionRoadsNumber)


        val change2 = AddRoadStateChange(
            Vec3(30.0, 0.0, 0.0) to Vec3(1.0, 0.0, 0.0),
            pos2,
            Vec3(30.0, 0.0, 30.0) to Vec3(-1.0, 0.0, 0.0),
            null
        )

        change2.apply(layout)
        assertEquals(2, layout.roads.size)
        assertEquals(2, layout.intersectionRoadsNumber)

        change2.revert(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(0, layout.intersectionRoadsNumber)

        change1.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, layout.intersections.size)
        assertEquals(0, layout.intersectionRoadsNumber)
    }

    @Test
    fun `create and undo triangular road cycle`() {
        val layout = Layout()

        val pos1 = Vec3(0.0, 0.0, 0.0)
        val pos2 = Vec3(30.0, 0.0, 0.0)
        val pos3 = Vec3(30.0, 0.0, 30.0)

        val change1 = AddRoadStateChange(
            pos1 to Vec3(1.0, 0.0, 0.0),
            null,
            pos2 to Vec3(-1.0, 0.0, 0.0),
            null
        )
        change1.apply(layout)

        val int2 = layout.intersections[1] ?: throw IllegalArgumentException("sosi")

        val change2 = AddRoadStateChange(
            pos2 to Vec3(1.0, 0.0, 0.0),
            int2,
            pos3 to Vec3(-1.0, 0.0, 0.0),
            null
        )
        change2.apply(layout)

        val int1 = layout.intersections[0] ?: throw IllegalArgumentException("sosi")
        val int3 = layout.intersections[2] ?: throw IllegalArgumentException("sosi")

        val change3 = AddRoadStateChange(
            pos1 to Vec3(1.0, 0.0, 0.0),
            int3,
            pos3 to Vec3(-1.0, 0.0, 0.0),
            int1
        )
        change3.apply(layout)

        assertEquals(3, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(6, layout.intersectionRoadsNumber)

        change3.revert(layout)
        assertEquals(2, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(2, layout.intersectionRoadsNumber)

        change2.revert(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertEquals(0, layout.intersectionRoadsNumber)

        change1.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, int1.intersectionRoads.size)
        assertEquals(0, int2.intersectionRoads.size)
        assertEquals(0, int3.intersectionRoads.size)
    }
}
