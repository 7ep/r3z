package coverosR3z.server

import coverosR3z.DEFAULT_DB_DIRECTORY
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import coverosR3z.misc.FileReader.Companion.read
import coverosR3z.misc.toStr
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.Socket
import kotlin.concurrent.thread

class ServerTests {

    private lateinit var client : SocketWrapper
    private lateinit var serverObject : Server
    private lateinit var au : FakeAuthenticationUtilities

    @Before
    fun init() {
        serverObject = Server(8080, DEFAULT_DB_DIRECTORY)
        au = FakeAuthenticationUtilities()
        thread { serverObject.startServer(au) }
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
        assertEquals(toStr(read("sample.html")!!), result.rawData)
    }

    /**
     * If the client asks for a file, give it
     * CSS edition
     */
    @Test
    fun testShouldGetFileResponse_CSS() {
        logSettings[LogTypes.TRACE] = true
        client.write("GET /sample.css HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("sample.css")!!), result.rawData)
    }

    /**
     * If the client asks for a file, give it
     * JS edition
     */
    @Test
    fun testShouldGetFileResponse_JS() {
        logSettings[LogTypes.TRACE] = true
        client.write("GET /sample.js HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("sample.js")!!), result.rawData)
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

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        client.write("GET /doesnotexist.html HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server does have, but it's not a suffix we recognize?
     * See ServerUtilties.handleUnknownFiles
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfoundUnknownSuffix() {
        client.write("GET /sample_template.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }


    /**
     * When we POST some data unauthorized, we should receive that message
     */
    @Test
    fun testShouldGetUnauthorizedResponseAfterPost() {
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.UNAUTHORIZED, result.statusCode)
    }

    /**
     * When we POST some data, we should receive a success message back
     */
    @Test
    fun testShouldGetSuccessResponseAfterPost() {
        au.getUserForSessionBehavior = { DEFAULT_USER }
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        val body = "project_entry=1&time_entry=2&detail_entry=nothing+to+say&date_entry=2012-06-20"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
    }

    /**
     * If we as client are connected but then close the connection from our side,
     * we should see a CLIENT_CLOSED_CONNECTION remark
     */
    @Test
    fun testShouldIndicateClientClosedConnection() {
        logSettings[LogTypes.TRACE] = true
        client.socket.shutdownOutput()

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(Verb.CLIENT_CLOSED_CONNECTION, result.verb)
    }

}