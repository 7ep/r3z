package coverosR3z.server

import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.domainobjects.NO_USER
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import coverosR3z.misc.FileReader
import coverosR3z.misc.toStr
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket


private lateinit var clientSocket : Socket
private lateinit var serverSocket : Socket
private lateinit var halfOpenServerSocket : ServerSocket
private lateinit var serverThread: Thread
private lateinit var server : SocketWrapper
private lateinit var client : SocketWrapper

/**
 * Get a server running, as a thread, so our tests
 * can run at the same time
 */
class ServerSocketInitializer: Runnable {

    /**
     * Run the server
     */
    override fun run() {
        halfOpenServerSocket = ServerSocket(12321)
        serverSocket = halfOpenServerSocket.accept()
    }
}

/**
 * This series of tests is mostly a relic from using tests
 * as a driver of design for HTTP protocol through sockets.
 *
 * That being the case, changes to this test class should
 * be minimized, unless we are working out some ideas about
 * how HTTP protocol works.
 */
class SocketTests {

    val tru = FakeTimeRecordingUtilities()
    val au = FakeAuthenticationUtilities()

    companion object{

        /**
         * When the class starts, get a server and client socket running
         * which will be shared amongst the tests
         */
        @BeforeClass @JvmStatic
        fun openSockets() {
            logSettings[LogTypes.TRACE] = true
            serverThread = Thread(ServerSocketInitializer())
            serverThread.start()
            clientSocket = Socket("localhost", 12321)
        }

        /**
         * Close the server socket, which closes all the sockets
         */
        @AfterClass @JvmStatic
        fun closeSockets() {
            halfOpenServerSocket.close()
        }
    }


    /**
     * Before each test, hook up fresh input and output streams
     * for the client and server socket
     */
    @Before
    fun init() {
        server = SocketWrapper(serverSocket, "server")
        client = SocketWrapper(clientSocket, "client")
    }

    @Test
    fun testSimpleConversation() {
        clientSocket.getOutputStream().write("Hello$CRLF".toByteArray())
        val br = BufferedReader(InputStreamReader(serverSocket.inputStream))
        val value = br.readLine()
        assertEquals(value, "Hello")
    }

    @Test
    fun test200Response() {
        client.write("GET / HTTP/1.1$CRLF")
        val serverInput = server.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            server.write("HTTP/1.1 200 OK$CRLF")
        }
        val response = client.readLine()

        assertEquals(response, "HTTP/1.1 200 OK")
    }

    @Test
    fun testShouldGetHtmlResponseFromServer(){
        client.write("GET / HTTP/1.1$CRLF")
        val serverInput = server.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            server.write("HTTP/1.1 200 OK$CRLF <!DOCTYPE html>$CRLF (the whole text of the page)$CRLF")
        }

        client.readLine()
        val response = client.readLine()

        assertTrue(response!!.contains("<!DOCTYPE html>"))
    }

    @Test
    fun testShouldGetHtmlFileResponseFromServer() {
        val webpage = toStr(FileReader.read("sample.html")!!)
        client.write("GET / HTTP/1.1\n")
        val serverInput = server.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            server.write("HTTP/1.1 200 OK\n <!DOCTYPE html>\n${webpage}\n")
        }

        repeat(2) {client.readLine()}
        var body = ""
        var nextLine = client.readLine()
        while (nextLine != "</html>") {
            body += "${nextLine}\n"
            nextLine = client.readLine()
        }
        body += nextLine
        assertEquals(webpage, body)
    }


    /**
     * Let's make sure our parsing process, which relies on a
     * particular protocol being held between the client and
     * server, holds true.
     * Hello?  Hello!
     */
    @Test
    fun testParsingProcess() {
        val expectedRequest = RequestData(Verb.GET, "page", emptyMap(), NO_USER, "foo", listOf("Cookie: blah", "Cookie: sessionId=foo", "Content-Length: 100"))
        client.write("GET /page HTTP/1.1$CRLF")
        client.write("Cookie: blah$CRLF")
        client.write("Cookie: sessionId=foo$CRLF")
        client.write("Content-Length: 100$CRLF")
        client.write(CRLF)

        val parseClientRequest = parseClientRequest(server, au)
        assertEquals("For this GET, we should receive certain data", expectedRequest, parseClientRequest)
    }

    /**
     * Like [testParsingProcess] but with a POST, and data
     */
    @Test
    fun testParsingProcess_POST() {
        val expectedRequest = RequestData(
            Verb.POST,
            "page",
            mapOf("foo" to "bar", "baz" to "feh"),
            NO_USER,
            "foo",
            listOf("Cookie: blah", "Cookie: sessionId=foo", "Content-Length: 100")
        )
        client.write("POST /page HTTP/1.1$CRLF")
        client.write("Cookie: blah$CRLF")
        client.write("Cookie: sessionId=foo$CRLF")
        client.write("Content-Length: 100$CRLF")
        client.write(CRLF)
        client.write("foo=bar&baz=feh")

        val parseClientRequest = parseClientRequest(server, au)
        assertEquals("For this POST, we should receive certain data", expectedRequest, parseClientRequest)
    }

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        // send a request to the server for something that doesn't exist
        client.write("GET /doesnotexist HTTP/1.1$CRLF$CRLF")

        // server - handle the request
        Server.handleRequest(server, au, tru)

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 404 NOT FOUND", statusline)
        assertTrue(headers.size > 1)
        assertEquals(notFoundHTML, body)
    }

    /**
     * What if we send a totally bad request, like GET BLAHBLAHBLAH
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_badRequest() {
        // send a request to the server for something that doesn't exist
        val badRequests = listOf(
                "GET BLAHBLAHBLAH HTTP/1.1$CRLF$CRLF",
                "GET$CRLF$CRLF",
                "BLAHBLAHBLAH HTTP/1.1$CRLF$CRLF",
                "HTTP/1.1$CRLF$CRLF",
        )
        for (request in badRequests) {
            client.write(request)

            // server - handle the request
            Server.handleRequest(server, au, tru)

            // client - read status line
            val statusline = client.readLine()
            val headers = getHeaders(client)
            val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
            val body = client.read(length)

            // assert all is well
            assertEquals("HTTP/1.1 400 BAD REQUEST", statusline)
            assertTrue(headers.size > 1)
            assertEquals(badRequestHTML, body)
        }
    }


    /**
     * When we ask for a file that requires reading a CSS file
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_CSS() {
        client.write("GET /sample.css HTTP/1.1$CRLF$CRLF")

        // server - handle the request
        Server.handleRequest(server, au, tru)

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertTrue(headers.size > 1)
        assertEquals("Content-Type: text/css", headers[1])
        val fileWeRead = FileReader.read("sample.css")
        assertEquals(toStr(fileWeRead!!), body)
    }

    /**
     * When we ask for a file that requires reading a JavaScript file
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_JS() {
        client.write("GET /sample.js HTTP/1.1$CRLF$CRLF")

        // server - handle the request
        Server.handleRequest(server, au, tru)

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertTrue(headers.size > 1)
        assertEquals("Content-Type: application/javascript", headers[1])
        val fileWeRead = FileReader.read("sample.js")
        assertEquals(toStr(fileWeRead!!), body)
    }


    /**
     * When we POST some data unauthorized, we should receive that message
     */
    @Test
    fun testShouldGetUnauthorizedResponseAfterPost() {
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF")
        client.write("Content-Length: 63$CRLF$CRLF")
        client.write("project_entry=projecta&time_entry=2&detail_entry=nothing+to+say")

        // server - handle the request
        Server.handleRequest(server, au, tru)

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert unauthorized
        assertEquals("HTTP/1.1 401 UNAUTHORIZED", statusline)
        assertTrue(headers.size > 1)
        assertEquals(unauthorizedHTML, body)
    }

    /**
     * When we POST some data, we should receive a success message back
     */
    @Test
    fun testShouldGetSuccessResponseAfterPost() {
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        client.write("Content-Length: 63$CRLF$CRLF")
        client.write("project_entry=1&time_entry=2&detail_entry=nothing+to+say")
        au.getUserForSessionBehavior = { DEFAULT_USER }

        // server - handle the request
        Server.handleRequest(server, au, tru)

        // client - read status line
        val statusline = client.readLine()

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
    }

    /**
     * If we as client are connected but then close the connection from our side,
     * we should see a CLIENT_CLOSED_CONNECTION remark
     */
    @Test
    fun testShouldIndicateClientClosedConnection() {
        logSettings[LogTypes.TRACE] = true
        client.socket.shutdownOutput()

        // server - handle the request
        val rd = Server.handleRequest(server, au, tru)

        assertEquals(Verb.CLIENT_CLOSED_CONNECTION, rd.verb)
    }


}