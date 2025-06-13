package ru.nsu.trafficsimulator.server

import OpenDriveWriter
import ru.nsu.trafficsimulator.logger
import ru.nsu.trafficsimulator.model.Layout
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
        val serverSocket = ServerSocket(port)
        logger.info { "Server started, listening on $port..." }

        while (clients.size < DISTRICTS_NUMBER) {
            val clientSocket: Socket = serverSocket.accept()

            logger.info { "Accept client: ${clientSocket.inetAddress.hostAddress}" }

            Thread {
                handleClient(clientSocket, clients.size)
            }.start()

            clients.add(clientSocket)
        }

        logger.info { "Server is no longer accepting connections" }

        val list = mutableListOf<Layout>()
        while (list.size < DISTRICTS_NUMBER) {
            list.add(receivedLayouts.take())
        }

        setResultLayout(Layout()) // should be LayoutMerger().merge(list)

        return resultLayout!!
    }


    private fun handleClient(clientSocket: Socket, districtId: Int) {
        clientSocket.use { client ->
            val xodrString = OpenDriveWriter().toString(serializeLayout(startLayout, districtId))

            println("tmp $xodrString")

            PrintWriter(client.getOutputStream(), true).use { writer ->
                writer.println(xodrString)
            }

            val resultXodr = StringBuilder()
            BufferedReader(InputStreamReader(client.getInputStream())).use { reader ->
                var line: String?

                while (true) {
                    line = reader.readLine() ?: break
                    if (line.isEmpty()) {
                        break
                    }
                    resultXodr.append(line).append("\n")
                }
            }

            val result = waitForResultLayout()

            val resultXodrString = OpenDriveWriter().toString(serializeLayout(result))

            PrintWriter(client.getOutputStream(), true).use { writer ->
                writer.println(resultXodrString)
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
