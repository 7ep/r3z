package coverosR3z.server

import coverosR3z.DEFAULT_DB_DIRECTORY
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import org.junit.Assert.*
import org.junit.Test
import java.net.Socket
import kotlin.concurrent.thread

class ServerTests {

    /**
     * When we start the server, we pass in a value for the port
     */
    @Test
    fun testShouldParsePortFromCLI() {
        val port : Int = Server.extractFirstArgumentAsPort(arrayOf("8080"))
        assertEquals(8080, port)
    }

    /**
     * If we try something and are unauthenticated,
     * receive a 401 error page
     */
    @Test
    fun testShouldReturnUnauthenticatedAs401Page() {
        logSettings[LogTypes.TRACE] = true
        val serverObject = Server(8080, DEFAULT_DB_DIRECTORY)
        thread { serverObject.startServer() }
        val clientSocket = Socket("localhost", 8080)
        val client = SocketWrapper(clientSocket, "client")

        client.write("POST /entertime HTTP/1.1$CRLF")
        val body = "test=test"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val statusline = client.readLine()

        serverObject.halfOpenServerSocket.close()
        assertEquals("HTTP/1.1 401 UNAUTHORIZED", statusline)
    }

}