package coverosR3z.server

import coverosR3z.DEFAULT_DB_DIRECTORY
import org.junit.Assert.*
import org.junit.Test
import java.net.Socket
import kotlin.concurrent.thread

class ServerTests {

    /**
     * If we pass in a path of "shutdown" when we are logged in
     * it should shut the server down
     */
    @Test
    fun testShouldShutdownServer() {
        val serverObject = Server(8080, DEFAULT_DB_DIRECTORY)
        val thread: Thread = thread {
            serverObject.startServer()
        }
        val clientSocket = Socket("localhost", 12321)
        val client = SocketWrapper(clientSocket, "client")
        client.write("GET /shutdown HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=blahblahblah$CRLF")

        thread.join()
        assertTrue(serverObject.halfOpenServerSocket.isClosed)
    }

    /**
     * When we start the server, we pass in a value for the port
     */
    @Test
    fun testShouldParsePortFromCLI() {
        val port : Int = Server.extractFirstArgumentAsPort(arrayOf("8080"))
        assertEquals(8080, port)
    }

    /**
     * If a general unhandled exception happens, we pass its message to the
     * client as a 500 error
     */
    @Test
    fun testShouldReturnErrorsAs500Page() {
        val serverObject = Server(8080, DEFAULT_DB_DIRECTORY)
        thread { serverObject.startServer() }
        val clientSocket = Socket("localhost", 12321)
        val client = SocketWrapper(clientSocket, "client")
        client.write("INVALID TEXT HERE$CRLF")

        val statusline = client.readLine()

        serverObject.halfOpenServerSocket.close()
        assertEquals(ResponseStatus.INTERNAL_SERVER_ERROR.value, statusline)
    }
}