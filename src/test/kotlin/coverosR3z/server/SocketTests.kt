package coverosR3z.server

import coverosR3z.logging.logInfo
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket


var clientSocket : Socket = Socket("localhost", 12321)
var serverSocket : Socket = Socket("localhost", 12321)

class HTTPHandler(): Runnable {

    override fun run() {
        val halfOpenServerSocket = ServerSocket(12321)
        serverSocket = halfOpenServerSocket.accept()
    }
}

class SocketTests() {

    @Before
    fun openSockets() {
        val server = Thread(HTTPHandler())
        server.start()
        clientSocket = Socket("localhost", 12321)
    }

    @After
    fun closeSockets() {
        serverSocket.close()
        clientSocket.close()
    }

    @Test
    fun testSimpleConversation() {
        clientSocket.getOutputStream().write("Hello\n".toByteArray())
        val br = BufferedReader(InputStreamReader(serverSocket.inputStream))
        val value = br.readLine()
        assertEquals(value, "Hello")
    }
}