//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
//
//class EditRoadStateChangeTest : BaseUndoRedoTest() {
//    @Test
//    fun `edit road lanes`() {
//        val road = createRoad(pos1, pos2)
//        val initialLeft = road.leftLane
//        val initialRight = road.rightLane
//
//        val change = EditRoadStateChange(road, 2, 3)
//
//        // Применяем изменение
//        change.apply(layout)
//        assertEquals(2, road.leftLane)
//        assertEquals(3, road.rightLane)
//
//        // Отменяем
//        change.revert(layout)
//        assertEquals(initialLeft, road.leftLane)
//        assertEquals(initialRight, road.rightLane)
//    }
//
//    @Test
//    fun `edit road should not affect connections`() {
//        val road1 = createRoad(pos1, pos2)
//        createRoad(pos2, pos3)
//
//        val middleIntersection = getIntersectionAt(pos2)
//        val initialConnections = middleIntersection.intersectionRoads.size
//
//        val change = EditRoadStateChange(road1, 2, 2)
//        change.apply(layout)
//
//        // Проверяем что соединения не изменились
//        assertEquals(initialConnections, middleIntersection.intersectionRoads.size)
//
//        change.revert(layout)
//        assertEquals(initialConnections, middleIntersection.intersectionRoads.size)
//    }
//}
