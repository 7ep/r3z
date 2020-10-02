package coverosR3z.server

import coverosR3z.logging.logInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

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
            assertTrue("Status message should be good", response.contains(expectedResponse))
        }
    }
}