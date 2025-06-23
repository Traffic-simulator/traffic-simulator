package route_generator_new

import ru.nsu.trafficsimulator.backend.utils.BuildingsParser
import OpenDriveReader
import opendrive.OpenDRIVE
import org.junit.jupiter.api.Test
import ru.nsu.trafficsimulator.backend.route.route_generator_new.BuildingTypes
import java.io.FileNotFoundException
import kotlin.test.assertEquals


class BuildingsParserTest {
    private val fileName : String = "simple_houses.xodr"


    @Test
    fun test() {
        val openDriveReader : OpenDriveReader = OpenDriveReader()
        val inputStream = this::class.java.classLoader.getResourceAsStream(fileName)
            ?: throw FileNotFoundException("Файл $fileName не найден в ресурсах")
        val openDrive : OpenDRIVE = openDriveReader.readUsingFileReader(inputStream)

        val parser : BuildingsParser = BuildingsParser(openDrive)
        val buildings = parser.getBuildings()
        var building = buildings[0]
        assertEquals(building.type, BuildingTypes.HOME)
        assertEquals(building.capacity, 100)
        assertEquals(building.currentPeople, 100)
        assertEquals(building.junctionId, "0")
        building = buildings[1]
        assertEquals(building.type, BuildingTypes.SHOPPING)
        assertEquals(building.capacity, 1000)
        assertEquals(building.currentPeople, 1000)
        assertEquals(building.junctionId, "1")

    }

}
