package coverosR3z.server

import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class ClientHandler(): Runnable {

    override fun run() {
        val serverSocket = ServerSocket(12321)
        val clientSocket = serverSocket.accept()
        val pw = PrintWriter(clientSocket.outputStream, true)
        val br = BufferedReader(InputStreamReader(clientSocket.inputStream))
        val line = br.readLine()
        println("Received: $line")
    }

}

class SocketTests() {

    @Test
    fun testShouldConnect() {

        val server = Thread(ClientHandler())
        server.start()
        val sock = Socket("localhost", 12321)
        sock.use {
            it.outputStream.write("hello socket world".toByteArray())
        }
        server.join()

        val server2 = Thread(ClientHandler())
        server2.start()
        val sock2 = Socket("localhost", 12321)
        sock2.use {
            it.outputStream.write("hello socket world".toByteArray())
        }
        server2.join()
    }

}