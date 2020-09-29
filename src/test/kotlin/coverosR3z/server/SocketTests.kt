package coverosR3z.server

import coverosR3z.logging.logInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket


class ClientHandler(): Runnable {

    override fun run() {
        val serverSocket = ServerSocket(12321)
        while(true) {
            val clientSocket = serverSocket.accept()
            val br = BufferedReader(InputStreamReader(clientSocket.inputStream))
            val line = br.readLine()
            println("Received: $line")
            if (line == "exit") {
                break
            }
            clientSocket.close()
        }
        serverSocket.close()
    }

}

class HTTPHandler(): Runnable {

    override fun run() {
        val serverSocket = ServerSocket(12321)
        val clientSocket = serverSocket.accept()
        clientSocket.use {
            val br = BufferedReader(InputStreamReader(it.inputStream))
            println("Server received: ${br.readLine()}")
            it.outputStream.write("HTTP/1.1 200 OK".toByteArray())
        }
        serverSocket.close()
    }

}

class SocketTests() {

    @Test
    fun testShouldConnect() {
        startAServer()

        for (x in 0..3) {
            talkToSocket(x)
        }

        `say goodbye to the socket`()
    }

    /**
     * If we talk to an HTTP server with the proper protocol,
     * it should reply politely
     */
    @Test
    fun testTryWithHttp() {
        val request = "GET / HTTP/1.1\n"
        val expectedResponse = "HTTP/1.1 200 OK"

        val server = Thread(HTTPHandler())
        server.start()

        val sock = Socket("localhost", 12321)
        sock.use {
            it.outputStream.write(request.toByteArray())
            val br = BufferedReader(InputStreamReader(it.inputStream))
            val response = br.readLine()
            println("Client received: $response")
            assertTrue("We should receive a good status message", response.contains(expectedResponse))
        }

    }


    private fun startAServer() {
        val server = Thread(ClientHandler())
        server.start()
    }


    private fun `say goodbye to the socket`() {
        val sock = Socket("localhost", 12321)
        sock.use {
            it.outputStream.write("exit".toByteArray())
        }
    }

    private fun talkToSocket(x: Int) {
        val sock = Socket("localhost", 12321)
        sock.use {
            it.outputStream.write("hello socket world $x".toByteArray())
        }
    }

}