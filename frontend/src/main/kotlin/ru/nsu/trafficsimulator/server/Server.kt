package ru.nsu.trafficsimulator.server

import OpenDriveReader
import OpenDriveWriter
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
import ru.nsu.trafficsimulator.model.LayoutMerger
import ru.nsu.trafficsimulator.serializer.Deserializer
import ru.nsu.trafficsimulator.serializer.serializeLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue

private const val DISTRICTS_NUMBER = 4

class Server(private val port: Int, private val startLayout: Layout) {
    private val clients = ArrayList<Socket>(DISTRICTS_NUMBER)
    private val receivedLayouts = ArrayBlockingQueue<Layout>(DISTRICTS_NUMBER)
    private var resultLayout: Layout? = null

    private val lock = Object()

    fun start(): Layout {
        val clientHandlers = mutableListOf<Thread>()

        val serverSocket = ServerSocket(port)
        logger.info { "Server started, listening on $port..." }

        while (clients.size < DISTRICTS_NUMBER) {
            val clientSocket: Socket = serverSocket.accept()

            logger.info { "Accept client: ${clientSocket.inetAddress.hostAddress}" }

            clients.add(clientSocket)

            val thread = Thread {
                handleClient(clientSocket, clients.size) // 1<=..<=DISTRICTS_NUMBER
            }
            thread.name = "Client handler thread: ${clients.size}"
            clientHandlers.add(thread)
            thread.start()
        }

        logger.info { "Server is no longer accepting connections" }

        val list = mutableListOf<Layout>()
        while (list.size < DISTRICTS_NUMBER) {
            list.add(receivedLayouts.take())
        }

        setResultLayout(LayoutMerger().merge(list)) // should be LayoutMerger().merge(list)

        clientHandlers.forEach { it.join() }
        logger.info { "Sent result layout to clients" }

        return resultLayout!!
    }


    private fun handleClient(clientSocket: Socket, districtId: Int) {
        clientSocket.use { client ->
            PrintWriter(client.getOutputStream(), true).use { writer ->
                BufferedReader(InputStreamReader(client.getInputStream())).use { reader ->
                    writer.println("DISTRICT: $districtId")

                    val initXodrString = OpenDriveWriter().toString(serializeLayout(startLayout, districtId))

                    writer.println(initXodrString)
                    writer.println("END OF INIT LAYOUT")
                    writer.flush()

                    val resultXodr = StringBuilder()
                    var line: String?

                    while (true) {
                        line = reader.readLine()
                        if (line == null) {
                            logger.warn { "Did not receive the whole layout from the client!" }
                            break
                        }
                        if (line == "END OF DISTRICT LAYOUT") {
                            break
                        }
                        resultXodr.append(line).append("\n")
                    }
                    logger.info { "Get layout from client with district ID: $districtId" }

                    val receivedOpenDrive =
                        OpenDriveReader().readUsingFileReader(resultXodr.toString().byteInputStream())
                    OpenDriveWriter().write(receivedOpenDrive, "client-$districtId.xodr")
                    receivedLayouts.add(Deserializer.deserialize(receivedOpenDrive))

                    val result = waitForResultLayout()

                    val resultXodrString = OpenDriveWriter().toString(serializeLayout(result))

                    writer.println(resultXodrString)
                    writer.println("END OF RESULT LAYOUT")
                    writer.flush()
                }
            }
        }
    }

    private fun waitForResultLayout(): Layout {
        synchronized(lock) {
            while (resultLayout == null) {
                lock.wait()
            }
            return resultLayout!!
        }
    }

    private fun setResultLayout(layout: Layout) {
        synchronized(lock) {
            resultLayout = layout
            lock.notifyAll()
        }
    }
}
