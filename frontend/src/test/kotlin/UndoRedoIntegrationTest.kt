//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import ru.nsu.trafficsimulator.editor.changes.AddRoadStateChange
//import ru.nsu.trafficsimulator.editor.changes.DeleteRoadStateChange
//import ru.nsu.trafficsimulator.editor.changes.EditRoadStateChange
//
//class UndoRedoIntegrationTest : BaseUndoRedoTest() {
//    @Test
//    fun `complex scenario with multiple changes`() {
//        // 1. Добавляем первую дорогу
//        val road1 = createRoad(pos1, pos2)
//        val addChange1 = AddRoadStateChange(
//            road1.startIntersection,
//            road1.startIntersection.position.toVec3() + rightDir,
//            road1.endIntersection,
//            road1.endIntersection.position.toVec3() + leftDir
//        )
//
//        // 2. Редактируем дорогу
//        val editChange = EditRoadStateChange(road1, 2, 2)
//        editChange.apply(layout)
//
//        // 3. Добавляем вторую дорогу
//        val road2 = createRoad(pos2, pos3)
//        val addChange2 = AddRoadStateChange(
//            road2.startIntersection,
//            road2.startIntersection.position.toVec3() + rightDir,
//            road2.endIntersection,
//            road2.endIntersection.position.toVec3() + leftDir
//        )
//
//        // 4. Удаляем первую дорогу
//        val deleteChange = DeleteRoadStateChange(road1)
//        deleteChange.apply(layout)
//
//        // Проверяем состояние
//        assertEquals(1, layout.roads.size)
//        assertEquals(1, getIntersectionAt(pos2).intersectionRoads.size)
//
//        // 5. Отменяем все по порядку
//        deleteChange.revert(layout)
//        addChange2.revert(layout)
//        editChange.revert(layout)
//        addChange1.revert(layout)
//
//        // Должны вернуться к пустому layout
//        assertEquals(0, layout.roads.size)
//        assertEquals(0, getIntersectionAt(pos1).intersectionRoads.size)
//        assertEquals(0, getIntersectionAt(pos2).intersectionRoads.size)
//        assertEquals(0, getIntersectionAt(pos3).intersectionRoads.size)
//    }
//}
