package trafficsim

import OpenDriveReader
import Simulator
import network.Waypoint
import vehicle.Direction
import java.util.*


val SEED: Long = 163


fun main() {
    println("Hello World!")

    val odr = OpenDriveReader()
    val simulator = initTown01Simulator(odr)

    MovingRectangle.jfxStart(simulator)
}
//
//fun initJunctionSimulator(odr: OpenDriveReader): Simulator {
//    //    val openDRIVE = odr.read("single_segment_road.xodr")
//    val openDRIVE = odr.read("UC_Simple-X-Junction.xodr")
//
//    println(openDRIVE.road.size)
//
//    val spawnDetails = ArrayList<Triple<String, String, Direction>>()
//    // spawnDetails.add(Triple("0", "1", Direction.BACKWARD))
//    spawnDetails.add(Triple("1", "1", Direction.BACKWARD))
//    spawnDetails.add(Triple("6", "1", Direction.BACKWARD))
//    spawnDetails.add(Triple("13", "1", Direction.BACKWARD))
//
//    val simulator: Simulator = Simulator(openDRIVE, SpawnDetails(spawnDetails), SEED);
//    return simulator
//}
//
//fun initSegmentRoadSimulator(odr: OpenDriveReader): Simulator {
//    val openDRIVE = odr.read("single_segment_road.xodr")
//    println(openDRIVE.road.size)
//
//    val spawnDetails = ArrayList<Triple<String, String, Direction>>()
//    spawnDetails.add(Triple("57", "1", Direction.BACKWARD))
//    spawnDetails.add(Triple("57", "2", Direction.BACKWARD))
//    spawnDetails.add(Triple("21", "-1", Direction.FORWARD))
//    spawnDetails.add(Triple("21", "-2", Direction.FORWARD))
//
//    val simulator = Simulator(openDRIVE, SpawnDetails(spawnDetails), SEED);
//    return simulator
//}
//
fun initTown01Simulator(odr: OpenDriveReader): Simulator {
    val openDRIVE = odr.read("Town01.xodr")
    println(openDRIVE.road.size)

    val spawnDetails = ArrayList<Waypoint>()
    val despawnDetails = ArrayList<Waypoint>()
    spawnDetails.add(Waypoint("6", "-1", Direction.FORWARD))
    despawnDetails.add(Waypoint("3", "-1", Direction.FORWARD))
    //spawnDetails.add(Triple("57", "2", Direction.BACKWARD))
    // spawnDetails.add(Triple("21", "-1", Direction.FORWARD))
    // spawnDetails.add(Triple("21", "-2", Direction.FORWARD))


    val simulator = Simulator(openDRIVE, spawnDetails, despawnDetails, SEED);
    return simulator
}
//
// fun initTrafficLightsSimulator(odr: OpenDriveReader): Simulator {
//     val openDRIVE = odr.read("simple/UC_Simple-X-Junction-TrafficLights.xodr")
//     println(openDRIVE.road.size)

//     val spawnDetails = ArrayList<Triple<String, String, Direction>>()

//     val simulator = Simulator(openDRIVE, SpawnDetails(spawnDetails), SEED);
//     return simulator
// }

