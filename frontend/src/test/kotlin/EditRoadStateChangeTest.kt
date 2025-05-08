import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Road

class EditRoadStateChangeDetailedTest {

    @Test
    fun `edit road lanes - basic test`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        // Добавляем дорогу
        val road = layout.addRoad(
            startPos,
            startPos.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            endPos,
            endPos.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val initialLeft = road.leftLane
        val initialRight = road.rightLane

        val change = EditRoadStateChange(road, 2, 3)

        change.apply(layout)
        assertEquals(2, road.leftLane)
        assertEquals(3, road.rightLane)

        change.revert(layout)
        assertEquals(initialLeft, road.leftLane)
        assertEquals(initialRight, road.rightLane)
    }

    @Test
    fun `multiple edit operations`() {
        val layout = Layout()
        val startPos = Intersection(1, Vec2(0.0, 0.0), 0.0)
        val endPos = Intersection(2, Vec2(10.0, 0.0), 0.0)

        val road = layout.addRoad(
            startPos,
            startPos.position.toVec3() + Vec3(1.0, 0.0, 0.0),
            endPos,
            endPos.position.toVec3() + Vec3(-1.0, 0.0, 0.0)
        )

        val initialLeft = road.leftLane
        val initialRight = road.rightLane

        val change1 = EditRoadStateChange(road, 2, 2)
        change1.apply(layout)
        assertEquals(2, road.leftLane)
        assertEquals(2, road.rightLane)

        val change2 = EditRoadStateChange(road, 3, 1)
        change2.apply(layout)
        assertEquals(3, road.leftLane)
        assertEquals(1, road.rightLane)

        change2.revert(layout)
        assertEquals(2, road.leftLane)
        assertEquals(2, road.rightLane)

        change1.revert(layout)
        assertEquals(initialLeft, road.leftLane)
        assertEquals(initialRight, road.rightLane)
    }
}
