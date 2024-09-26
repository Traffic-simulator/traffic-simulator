package org.example


fun main() {
    println("Hello World!")

    val odr = OpenDriveReader()
    val openDRIVE = odr.read("Town01.xodr")

    println(openDRIVE.road.size)

    val odw = OpenDriveWriter()
    odw.write(openDRIVE, "Town01_processed.xodr")
}