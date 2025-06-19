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
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    fun connect(serverHost: String, serverPort: Int): Layout {
        if (connected) {
            throw IllegalStateException("Client is already connected to a server")
        }
        currentSocket = Socket(serverHost, serverPort).also {
            outputStream = it.getOutputStream()
            inputStream = it.getInputStream()
        }
        connected = true
        return try {
            val initialLayout = receiveInitialLayout(currentSocket!!)
            initialLayout
        } catch (e: Exception) {
            disconnect()
            throw IllegalStateException("Error while connecting to $serverHost:$serverPort", e)
        }
    }

    fun sendLayout(layout: Layout): Layout {
        if (!connected || currentSocket == null) {
            throw IllegalStateException("Client is not connected to any server")
        }
        return try {
            sendLayoutToServer(layout)
            val resultLayout = receiveResultLayout()
            resultLayout
        } catch (e: Exception) {
            disconnect()
            throw e
        }
    }

    private fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            currentSocket?.close()
        } catch (e: Exception) {
            logger.error(e) { "Error while disconnecting" }
        } finally {
            currentSocket = null
            inputStream = null
            outputStream = null
            connected = false
            districtId = -1
        }
    }

    private fun receiveResultLayout(): Layout {
        val input = inputStream ?: throw IllegalStateException("No input stream available")
        BufferedReader(InputStreamReader(input)).use { reader ->
            val xodrString = StringBuilder()
            var line: String?
            while (true) {
                line = reader.readLine() ?: throw IllegalStateException("Unexpected end of stream")
                if (line == "END OF RESULT LAYOUT") break
                xodrString.append(line).append("\n")
            }
            val xodr = OpenDriveReader().readUsingFileReader(xodrString.toString().byteInputStream())
            return Deserializer.deserialize(xodr)
        }
    }

    private fun receiveInitialLayout(socket: Socket): Layout {
        val input = inputStream ?: throw IllegalStateException("No input stream available")
        BufferedReader(InputStreamReader(input)).use { reader ->
            val districtLine = reader.readLine() ?: throw IllegalStateException("No district ID received")
            districtId = districtLine.removePrefix("DISTRICT: ").toInt()

            val xodrString = StringBuilder()
            var line: String?
            while (true) {
                line = reader.readLine() ?: throw IllegalStateException("Unexpected end of stream")
                if (line == "END OF INIT LAYOUT") break
                xodrString.append(line).append("\n")
            }


            val xodr = OpenDriveReader().readUsingFileReader(xodrString.toString().byteInputStream())
            return Deserializer.deserialize(xodr)
        }
    }

    private fun sendLayoutToServer(layout: Layout) {
        val output = outputStream ?: throw IllegalStateException("No output stream available")
        PrintWriter(output, true).use { writer ->
            val xodrString = OpenDriveWriter().toString(serializeLayout(layout))
            writer.println(xodrString)
            writer.println("END OF DISTRICT LAYOUT")
            writer.flush()
        }
    }

    fun getConnected(): Boolean {
        return connected
    }
}
