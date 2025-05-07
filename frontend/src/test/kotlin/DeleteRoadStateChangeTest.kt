import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.editor.changes.DeleteRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout

class DeleteRoadStateChangeDetailedTest {

    @Test
    fun `delete existing road - basic test`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val road = layout.addRoad(
            startPos,
            startPos.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            endPos,
            endPos.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val change = DeleteRoadStateChange(road)

        change.apply(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, startPos.intersectionRoads.size)
        assertEquals(0, endPos.intersectionRoads.size)

        change.revert(layout)
        assertEquals(1, layout.roads.size)
        assertTrue(layout.roads.containsKey(road.id))
    }

    @Test
    fun `delete road should clean up connections`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val road = layout.addRoad(
            startPos,
            startPos.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            endPos,
            endPos.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val initialStartConnections = startPos.intersectionRoads.size
        val initialEndConnections = endPos.intersectionRoads.size

        val change = DeleteRoadStateChange(road)
        change.apply(layout)

        assertEquals(0, startPos.intersectionRoads.size)
        assertEquals(0, endPos.intersectionRoads.size)

        change.revert(layout)

        assertEquals(initialStartConnections, startPos.intersectionRoads.size)
        assertEquals(initialEndConnections, endPos.intersectionRoads.size)
    }

    @Test
    fun `delete middle road in chain`() {
        val layout = Layout()
        val pos1 = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val pos2 = Intersection(2, Vec2(10.0, 0.0), 0.0)
        val pos3 = Intersection(3, Vec2(20.0, 0.0), 0.0)

        val road1 = layout.addRoad(
            pos1,
            pos1.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            pos2,
            pos2.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )
        val road2 = layout.addRoad(
            pos2,
            pos2.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            pos3,
            pos3.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )


        val change = DeleteRoadStateChange(road1)
        change.apply(layout)

        assertEquals(1, layout.roads.size)
        assertEquals(layout.roads.values.first().id, road2.id)
        change.revert(layout)

        assertEquals(2, layout.roads.size)
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

        val road1 = layout.addRoad(
            intersection1,
            Vec3(1.0, 0.0, 0.0),
            intersection2,
            Vec3(-1.0, 0.0, 0.0),
        )

        val road2 = layout.addRoad(
            intersection2,
            Vec3(1.0, 0.0, 0.0),
            intersection3,
            Vec3(-1.0, 0.0, 0.0),
        )

        val road3 = layout.addRoad(
            intersection3,
            Vec3(1.0, 0.0, 0.0),
            intersection1,
            Vec3(-1.0, 0.0, 0.0),
        )

        val change1 = DeleteRoadStateChange(road1)
        val change2 = DeleteRoadStateChange(road2)
        val change3 = DeleteRoadStateChange(road3)

        change1.apply(layout)
        change2.apply(layout)
        change3.apply(layout)

        assertEquals(0, layout.roads.size)
        assertEquals(0, intersection1.intersectionRoads.size)
        assertEquals(0, intersection2.intersectionRoads.size)
        assertEquals(0, intersection3.intersectionRoads.size)

        change3.revert(layout)

        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)

        change2.revert(layout)

        assertEquals(2, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(2, layout.intersectionRoads.size)

        change1.revert(layout)
        assertEquals(3, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(6, layout.intersectionRoads.size)
    }
}
