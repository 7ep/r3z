package coverosR3z.server

import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

var I_AM_READY_TO_ROCK = false

class ClientHandler(private val clientSocket: Socket): Runnable {
    private val connectionId: Int

    init {
        connectionId = ++numConnections
        println("Handling connection, #$connectionId")
    }

    override fun run() {
        val pw = PrintWriter(clientSocket.outputStream, true)
        val br = BufferedReader(InputStreamReader(clientSocket.inputStream))
        while (true) {
            val line = br.readLine() ?: break
            println("Received: $line")
            pw.write("$line\n")
            pw.flush()
            if (line == "exit") break
        }
        br.close()
        pw.close()
        clientSocket.close()
        println("Closing connection, #$connectionId")
    }

    private companion object {
        var numConnections = 0
    }
}

class MyServer() : Runnable {
    fun main(args: Array<String>) {
        val serverSocket = ServerSocket(12321)
        try {
            while (true) {
                // announce to the heavens that we are rockin'
                I_AM_READY_TO_ROCK = true
                Thread(ClientHandler(serverSocket.accept())).start()
            }
        } finally {
            serverSocket.close()
            println("Closing server socket")
        }
    }

    override fun run() {
        main(emptyArray())
    }
}

class SocketTests() {

    /**
     * The basic happy path
     */
    @Test
    fun testEchoServerShouldReply() {
        Thread(MyServer()).start()
        while(!I_AM_READY_TO_ROCK) {
            Thread.sleep(5)
        }
        val sock = Socket("localhost", 12321)
        sock.use {
            it.outputStream.write("hello socket world".toByteArray())
        }
    }

}