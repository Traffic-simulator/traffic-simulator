package trafficsim

import OpenDriveReader
import Simulator
import SpawnDetails
import vehicle.Direction
import java.util.*

fun main() {
    println("Hello World!")

    val odr = OpenDriveReader()
    val simulator = initJunctionSimulator(odr)

    MovingRectangle.jfxStart(simulator)
}

fun initJunctionSimulator(odr: OpenDriveReader): Simulator {
    //    val openDRIVE = odr.read("single_segment_road.xodr")
    val openDRIVE = odr.read("UC_Simple-X-Junction.xodr")

    println(openDRIVE.road.size)

    val spawnDetails = ArrayList<Triple<String, String, Direction>>()
    spawnDetails.add(Triple("0", "1", Direction.BACKWARD))
    spawnDetails.add(Triple("1", "1", Direction.BACKWARD))
    spawnDetails.add(Triple("6", "1", Direction.BACKWARD))
    spawnDetails.add(Triple("13", "1", Direction.BACKWARD))

    val simulator: Simulator = Simulator(openDRIVE, SpawnDetails(spawnDetails), 228);
    return simulator
}

fun initSegmentRoadSimulator(odr: OpenDriveReader): Simulator {
    val openDRIVE = odr.read("single_segment_road.xodr")
    println(openDRIVE.road.size)

    val spawnDetails = ArrayList<Triple<String, String, Direction>>()
    spawnDetails.add(Triple("57", "1", Direction.BACKWARD))
    spawnDetails.add(Triple("57", "2", Direction.BACKWARD))
    spawnDetails.add(Triple("21", "-1", Direction.FORWARD))
    spawnDetails.add(Triple("21", "-2", Direction.FORWARD))

    val simulator = Simulator(openDRIVE, SpawnDetails(spawnDetails), 228);
    return simulator
}


