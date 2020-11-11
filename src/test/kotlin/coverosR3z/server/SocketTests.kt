package coverosR3z.server

import coverosR3z.authentication.CurrentUser
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.logging.logDebug
import coverosR3z.server.ServerUtilities.Companion.contentLengthRegex
import coverosR3z.server.ServerUtilities.Companion.getHeaders
import coverosR3z.templating.FileReader
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


lateinit var clientSocket : Socket
lateinit var serverSocket : Socket
lateinit var halfOpenServerSocket : ServerSocket
lateinit var serverThread: Thread
lateinit var server : SocketWrapper
lateinit var client : SocketWrapper

class ServerSocketInitializer(): Runnable {

    override fun run() {
        halfOpenServerSocket = ServerSocket(12321)
        serverSocket = halfOpenServerSocket.accept()
    }
}

class SocketTests() {

    val tru = FakeTimeRecordingUtilities()
    val au = FakeAuthenticationUtilities()

    companion object{
        @BeforeClass @JvmStatic
        fun openSockets() {
            serverThread = Thread(ServerSocketInitializer())
            serverThread.start()
            clientSocket = Socket("localhost", 12321)
        }

        @AfterClass @JvmStatic
        fun closeSockets() {
            halfOpenServerSocket.close()
        }
    }

    @Before
    fun init() {
        server = SocketWrapper(serverSocket, "server")
        client = SocketWrapper(clientSocket, "client")
    }

    @Test
    fun testSimpleConversation() {
        clientSocket.getOutputStream().write("Hello\n".toByteArray())
        val br = BufferedReader(InputStreamReader(serverSocket.inputStream))
        val value = br.readLine()
        assertEquals(value, "Hello")
    }

    @Test
    fun test200Response() {
        client.write("GET / HTTP/1.1\n")
        val serverInput = server.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            server.write("HTTP/1.1 200 OK\n")
        }
        val response = client.readLine()

        assertEquals(response, "HTTP/1.1 200 OK")
    }

    @Test
    fun testShouldGetHtmlResponseFromServer(){
        client.write("GET / HTTP/1.1\n")
        val serverInput = server.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            server.write("HTTP/1.1 200 OK\n <!DOCTYPE html>\n (the whole text of the page)\n")
        }

        client.readLine()
        val response = client.readLine()

        assertTrue(response.contains("<!DOCTYPE html>"))
    }

    @Test
    fun testShouldGetHtmlFileResponseFromServer() {
        val webpage = FileReader.read("sample.html")
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
     * This takes [testShouldGetHtmlFileResponseFromServer] and
     * refactors it a bit so we can more easily separate the
     * client and server code
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_MaybeRefactored() {
        // client - send a request to the server for the page we want
        val pageDesired = "sample.html"
        client.write("GET /$pageDesired HTTP/1.1\n\n")

        executeServerProcess()

        // client - read status line
        val statusline = client.readLine()
        // client - read the headers
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        // client - read the body
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertTrue(headers.size > 1)
        val fileWeRead = FileReader.read(pageDesired)
        assertEquals(fileWeRead, body)
    }

    /**
     * Just for the test code - very similar to what is happening in [SocketCommunication],
     * except that this way we have access to the inside variables
     */
    private fun executeServerProcess() {
        // server - handle the request
        val requestData = ServerUtilities.parseClientRequest(server, au)

        // now that we know who the user is (if they authenticated) we can update the current user
        val truWithUser = tru.changeUser(CurrentUser(requestData.user))

        val responseData = ServerUtilities(au, truWithUser).handleRequestAndRespond(requestData)
        ServerUtilities.returnData(server, responseData)
    }

    /**
     * What if we ask for nothing? like, GET / HTTP/1.1
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_AskForNothing() {
        // client - send a request to the server for the page we want
        client.write("GET / HTTP/1.1\n\n")

        // server - handle the request
        executeServerProcess()

        // client - read status line
        val statusline = client.readLine()
        // client - read the headers
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        // client - read the body
        val body = client.read(length)
        logDebug(body)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertTrue(headers.size > 1)
    }

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        // send a request to the server for something that doesn't exist
        client.write("GET /doesnotexist HTTP/1.1\n\n")

        // server - handle the request
        executeServerProcess()

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 404 NOT FOUND", statusline)
        assertTrue(headers.size > 1)
        assertEquals("Content-Length: 194", headers[0])
        val fileWeRead = FileReader.read("404error.html")
        assertEquals(fileWeRead, body)
    }

    /**
     * What if we send a totally bad request, like GET BLAHBLAHBLAH
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_badRequest() {
        // send a request to the server for something that doesn't exist
        val badRequests = listOf(
                "GET BLAHBLAHBLAH HTTP/1.1\n\n",
                "GET\n\n",
                "BLAHBLAHBLAH HTTP/1.1\n\n",
                "HTTP/1.1\n\n",
        )
        for (request in badRequests) {
            client.write(request)

            // server - handle the request
            executeServerProcess()

            // client - read status line
            val statusline = client.readLine()
            val headers = getHeaders(client)
            val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
            val body = client.read(length)

            // assert all is well
            assertEquals("HTTP/1.1 400 BAD REQUEST", statusline)
            assertTrue(headers.size > 1)
            assertEquals("Content-Length: 194", headers[0])
            val fileWeRead = FileReader.read("400error.html")
            assertEquals(fileWeRead, body)
        }
    }

    /**
     * When we ask for a file that requires reading a template
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_template() {
        client.write("GET /sample_template.utl HTTP/1.1\n\n")

        // server - handle the request
        executeServerProcess()

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertTrue(headers.size > 1)
        val fileWeRead = FileReader.read("sample.html")
        assertEquals(fileWeRead, body)
    }

    /**
     * When we ask for a file that requires reading a CSS file
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_CSS() {
        client.write("GET /sample.css HTTP/1.1\n\n")

        // server - handle the request
        executeServerProcess()

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
        assertEquals(fileWeRead, body)
    }

    /**
     * When we ask for a file that requires reading a JavaScript file
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_JS() {
        client.write("GET /sample.js HTTP/1.1\n\n")

        // server - handle the request
        executeServerProcess()

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
        assertEquals(fileWeRead, body)
    }


    /**
     * When we POST some data, we should receive a success message back
     */
    @Test
    fun testShouldGetSuccessResponseAfterPost() {
        client.write("POST /${TargetPage.ENTER_TIME.value} HTTP/1.1\n")
        client.write("Content-Length: 63\n\n")
        client.write("project_entry=projecta&time_entry=2&detail_entry=nothing+to+say")

        // server - handle the request
        executeServerProcess()

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 401 UNAUTHORIZED", statusline)
        assertTrue(headers.size > 1)
        assertEquals(FileReader.readNotNull("401error.html"), body)
    }


}