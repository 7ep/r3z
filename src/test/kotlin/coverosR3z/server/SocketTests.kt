package coverosR3z.server

import coverosR3z.logging.logInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

val bagOfSockets = mutableListOf<Socket>()

class ClientHandler(): Runnable {

    override fun run() {
        val serverSocket = ServerSocket(12321)
        while(true) {
            val clientSocket = serverSocket.accept()
            bagOfSockets.add(clientSocket)
            val br = BufferedReader(InputStreamReader(clientSocket.inputStream))
            val line = br.readLine()
            println("Received: $line")
            if (line == "exit") {
                break
            }
//            clientSocket.close()
        }
        serverSocket.close()
    }

}

class HTTPHandler(): Runnable {

    override fun run() {
        val serverSocket = ServerSocket(12321)

        val clientSocket = serverSocket.accept()
        bagOfSockets.add(clientSocket)
        val br = BufferedReader(InputStreamReader(clientSocket.inputStream))
        val line = "HTTP/1.1 200 OK"
        println("Received: $line")
        clientSocket.outputStream.write(line.toByteArray())

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
        `show all the sockets`()
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

        val br = BufferedReader(InputStreamReader())
        val response =
            talkToHTTPSocket(request)




        assertTrue("We should receive a good status message", response.contains(expectedResponse))
    }






    private fun startAServer() {
        val server = Thread(ClientHandler())
        server.start()
    }


    private fun `show all the sockets`() {
        logInfo(bagOfSockets.joinToString { s -> s.toString() })
    }


    private fun `say goodbye to the socket`() {
        val sock = Socket("localhost", 12321)
        bagOfSockets.add(sock)
        sock.use {
            it.outputStream.write("exit".toByteArray())
        }
    }

    private fun talkToSocket(x: Int) {
        val sock = Socket("localhost", 12321)
        bagOfSockets.add(sock)
        sock.use {
            it.outputStream.write("hello socket world $x".toByteArray())
        }
    }

    private fun talkToHTTPSocket(request : String) : String{
        val sock = Socket("localhost", 12321)
        var response = ""
        sock.use {
            it.outputStream.write("$request".toByteArray())
            response = BufferedReader(InputStreamReader(it.inputStream)).readLine()
        }
        return response
    }



}