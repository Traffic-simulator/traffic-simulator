package trafficsim

import OpenDriveReader
import Simulator
import java.util.*
fun main() {

    val rnd = Random()

    println("Hello World!")

    val odr = OpenDriveReader()
    val openDRIVE = odr.read("single_road.xodr")

    println(openDRIVE.road.size)

    val simulator: Simulator = Simulator(openDRIVE);

    MovingRectangle.jfxStart(simulator)

}
