//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import ru.nsu.trafficsimulator.editor.changes.DeleteRoadStateChange
//
//class DeleteRoadStateChangeTest : BaseUndoRedoTest() {
//    @Test
//    fun `delete single road`() {
//        val road = createRoad(pos1, pos2)
//        val initialConnections = road.startIntersection.intersectionRoads.size +
//            road.endIntersection.intersectionRoads.size
//
//        val change = DeleteRoadStateChange(road)
//
//        // Удаляем
//        change.apply(layout)
//        assertEquals(0, layout.roads.size)
//        assertEquals(0, road.startIntersection.intersectionRoads.size)
//        assertEquals(0, road.endIntersection.intersectionRoads.size)
//
//        // Восстанавливаем
//        change.revert(layout)
//        assertEquals(1, layout.roads.size)
//        assertEquals(initialConnections,
//            road.startIntersection.intersectionRoads.size +
//                road.endIntersection.intersectionRoads.size)
//    }
//
//    @Test
//    fun `delete middle road in chain`() {
//        // Создаем две соединенные дороги
//        val road1 = createRoad(pos1, pos2)
//        val road2 = createRoad(pos2, pos3)
//
//        val middleIntersection = getIntersectionAt(pos2)
//        val initialConnections = middleIntersection.intersectionRoads.size
//
//        val change = DeleteRoadStateChange(road1)
//
//        // Удаляем первую дорогу
//        change.apply(layout)
//        assertEquals(1, layout.roads.size)
//        assertEquals(initialConnections - 1, middleIntersection.intersectionRoads.size)
//
//        // Восстанавливаем
//        change.revert(layout)
//        assertEquals(2, layout.roads.size)
//        assertEquals(initialConnections, middleIntersection.intersectionRoads.size)
//    }
//}
