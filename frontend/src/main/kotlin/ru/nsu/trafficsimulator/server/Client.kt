package ru.nsu.trafficsimulator.server

import OpenDriveReader
import OpenDriveWriter
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.serializer.serializeLayout
import java.io.*
import java.net.Socket

class Client {
    private var connected = false
    private var currentSocket: Socket? = null
    private var districtId: Int = -1
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    fun connect(serverHost: String, serverPort: Int): Layout {
        if (connected) {
            throw IllegalStateException("Client is already connected to a server")
        }

        try {
            currentSocket = Socket(serverHost, serverPort).also {
                reader = BufferedReader(InputStreamReader(it.getInputStream()))
                writer = PrintWriter(it.getOutputStream(), true)
            }
            connected = true

            return receiveInitialLayout()
        } catch (e: Exception) {
            disconnect()
            throw IllegalStateException("Error while connecting to $serverHost:$serverPort", e)
        }
    }

    fun sendLayout(layout: Layout): Layout {
        if (!connected) {
            throw IllegalStateException("Client is not connected to any server")
        }

        return try {
            sendLayoutToServer(layout)
            receiveResultLayout()
        } catch (e: Exception) {
            disconnect()
            throw IllegalStateException("Failed to send layout", e)
        }
    }

    private fun disconnect() {
        try {
            reader?.close()
            writer?.close()
            currentSocket?.close()
        } catch (e: Exception) {
            logger.error(e) { "Error while disconnecting" }
        } finally {
            currentSocket = null
            reader = null
            writer = null
            connected = false
            districtId = -1
        }
    }

    private fun receiveResultLayout(): Layout {
        val r = reader ?: throw IllegalStateException("No reader available")

        val xodrString = StringBuilder()
        while (true) {
            val line = r.readLine() ?: throw IllegalStateException("Unexpected end of stream")
            if (line == "END OF RESULT LAYOUT") break
            xodrString.append(line).append("\n")
        }

        val xodr = OpenDriveReader().readUsingFileReader(xodrString.toString().byteInputStream())
        return Deserializer.deserialize(xodr)
    }

    private fun receiveInitialLayout(): Layout {
        val r = reader ?: throw IllegalStateException("No reader available")

        val districtLine = r.readLine() ?: throw IllegalStateException("No district ID received")
        districtId = districtLine.removePrefix("DISTRICT: ").toInt()

        val xodrString = StringBuilder()
        while (true) {
            val line = r.readLine() ?: throw IllegalStateException("Unexpected end of stream")
            if (line == "END OF INIT LAYOUT") break
            xodrString.append(line).append("\n")
        }

        val xodr = OpenDriveReader().readUsingFileReader(xodrString.toString().byteInputStream())
        return Deserializer.deserialize(xodr)
    }

    private fun sendLayoutToServer(layout: Layout) {
        val w = writer ?: throw IllegalStateException("No writer available")

        val xodrString = OpenDriveWriter().toString(serializeLayout(layout))
        w.println(xodrString)
        w.println("END OF DISTRICT LAYOUT")
        w.flush()
    }

    fun getConnected(): Boolean {
        return connected
    }
}
