package coverosR3z.server

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
        }
        serverSocket.close()
    }

}

class SocketTests() {

    @Test
    fun testShouldConnect() {
        val server = Thread(ClientHandler())
        server.start()

        for (x in 0..5) {
            val sock = Socket("localhost", 12321)
            sock.use {
                it.outputStream.write("hello socket world $x".toByteArray())
            }
        }

        val sock = Socket("localhost", 12321)
        sock.use {
            it.outputStream.write("exit".toByteArray())
        }
    }


}