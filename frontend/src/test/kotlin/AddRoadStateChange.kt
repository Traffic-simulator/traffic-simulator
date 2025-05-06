import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
import ru.nsu.trafficsimulator.math.Vec2
import ru.nsu.trafficsimulator.math.Vec3
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.Intersection
import ru.nsu.trafficsimulator.model.Road

class AddRoadStateChangeTest {
    @Test
    fun `add road between two intersections`() {
        val layout =  Layout()
        val startPos = Intersection(1, Vec2(20.0, 10.0), 0.0)
        val endPos = Intersection(2, Vec2(-20.0, -10.0), 0.0)

        val change = AddRoadStateChange(startPos, Vec3(-2.0, 0.0, 0.0), endPos, Vec3(2.0, 0.0, 0.0))

        change.apply(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)
        change.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)
    }


    @Test
    fun `add two road between three intersections`() {
        val layout =  Layout()
        val inter1 = Intersection(1, Vec2(20.0, 10.0), 0.0)
        val inter2 = Intersection(2, Vec2(-20.0, -10.0), 0.0)
        val inter3 = Intersection(3, Vec2(40.0, -10.0), 0.0)

        val road1 = layout.addRoad(inter1, Vec3(-2.0, 0.0, 0.0), inter2, Vec3(2.0, 0.0, 0.0))
        val road2 = layout.addRoad(inter3, Vec3(-2.0, 0.0, 0.0), inter2, Vec3(2.0, 0.0, 0.0))

        layout.deleteRoad(road2)
        layout.deleteRoad(road1)

        val change1 = AddRoadStateChange(inter1, Vec3(-2.0, 0.0, 0.0), inter2, Vec3(2.0, 0.0, 0.0))
        val change2 = AddRoadStateChange(inter3, Vec3(-2.0, 0.0, 0.0), inter2, Vec3(2.0, 0.0, 0.0))

        change1.apply(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)
        change2.apply(layout)
        assertEquals(2, layout.roads.size)
        assertEquals(3, layout.intersections.size)
        assertEquals(2, layout.intersectionRoads.size)
        change2.revert(layout)
        assertEquals(1, layout.roads.size)
        assertEquals(2, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)
        change1.revert(layout)
        assertEquals(0, layout.roads.size)
        assertEquals(0, layout.intersections.size)
        assertEquals(0, layout.intersectionRoads.size)
    }

//    @Test
//    fun `add road and check intersection connections`() {
//        // Сначала создаем первую дорогу
//        val road1 = createRoad(pos1, pos2)
//
//        // Добавляем вторую дорогу
//        val start = getIntersectionAt(pos2)
//        val end = getIntersectionAt(pos3)
//
//        val startDir = start.position.toVec3() + rightDir
//        val endDir = end.position.toVec3() + leftDir
//
//        val change = AddRoadStateChange(start, startDir, end, endDir)
//
//        assertDoesNotThrow { change.apply(layout) }
//
//        val road2 = layout.roads.values.first { it != road1 }
//
//        // Проверяем что у среднего перекрестка 2 соединения
//        assertEquals(2, start.intersectionRoads.size)
//
//        assertEquals(1, end.intersectionRoads.size)
//
//        // Отменяем
//        assertDoesNotThrow { change.revert(layout) }
//        assertEquals(1, start.intersectionRoads.size)
//    }
}
