package coverosR3z.server

import coverosR3z.DEFAULT_DB_DIRECTORY
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.Socket
import kotlin.concurrent.thread

class ServerTests {

    private lateinit var client : SocketWrapper
    private lateinit var serverObject : Server

    @Before
    fun init() {
        serverObject = Server(8080, DEFAULT_DB_DIRECTORY)
        thread { serverObject.startServer() }
        val clientSocket = Socket("localhost", 8080)
        client = SocketWrapper(clientSocket, "client")
    }

    @After
    fun cleanup() {
        serverObject.halfOpenServerSocket.close()
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
     * If we try something and are unauthenticated,
     * receive a 401 error page
     */
    @Test
    fun testShouldReturnUnauthenticatedAs401Page() {
        client.write("POST /entertime HTTP/1.1$CRLF")
        val body = "test=test"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 401 UNAUTHORIZED", statusline)
    }

    /**
     * If we ask for the homepage, we'll get a 200 OK
     */
    @Test
    fun testShouldGet200Response() {
        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 200 OK", statusline)
    }

    /**
     * If the client asks for a file, give it
     */
    @Test
    fun testShouldGetFileResponse() {
        logSettings[LogTypes.TRACE] = true
        client.write("GET /sample.html HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
    }


    /**
     * Action for an invalid request
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_BadRequest() {
        client.write("INVALID /test.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.BAD_REQUEST, result.statusCode)
    }

}