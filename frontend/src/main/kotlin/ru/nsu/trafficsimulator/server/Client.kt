package ru.nsu.trafficsimulator.server

import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import java.net.Socket
import OpenDriveWriter
import ru.nsu.trafficsimulator.serializer.serializeLayout
import ru.nsu.trafficsimulator.serializer.Deserializer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import OpenDriveReader

class Client {
    private var connected = false
    private var currentSocket: Socket? = null

    fun connect(serverHost: String, serverPort: Int): Layout {
        if (connected) {
            throw IllegalStateException("Client is already connected to a server")
        }
        val socket = Socket(serverHost, serverPort)
        currentSocket = socket
        connected = true
        return try {
            val initialLayout = receiveLayout(socket)
            initialLayout
        } catch (e: Exception) {
            disconnect()
            throw IllegalStateException("Error while connecting to $serverHost:$serverPort", e)
        }
    }

    private fun disconnect() {
        try {
            currentSocket?.close()
        } catch (e: Exception) {
            logger.error(e) { "Error while disconnecting" }
        } finally {
            currentSocket = null
            connected = false
        }
    }

    fun sendLayout(layout: Layout): Layout {
        if (!connected || currentSocket == null) {
            throw IllegalStateException("Client is not connected to any server")
        }
        return try {
            sendLayoutToServer(layout)
            val resultLayout = receiveLayout(currentSocket!!)
            resultLayout
        } catch (e: Exception) {
            disconnect()
            throw e
        }
    }

    private fun receiveLayout(socket: Socket): Layout {
        val xodrString = StringBuilder()
        BufferedReader(InputStreamReader(socket.getInputStream())).use { reader ->
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                if (line.isEmpty()) {
                    break
                }
                xodrString.append(line).append("\n")
            }
        }

        val xodr = OpenDriveReader().read(xodrString.toString())
        return Deserializer.deserialize(xodr)
    }

    private fun sendLayoutToServer(layout: Layout) {
        val resultXodrString = OpenDriveWriter().toString(serializeLayout(layout))
        if (currentSocket != null) {
            PrintWriter(currentSocket!!.getOutputStream(), true).use { writer ->
                writer.println(resultXodrString)
            }
        }
    }
}
