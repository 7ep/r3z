package coverosR3z.server

import coverosR3z.logging.logDebug
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.server.ServerUtilities.Companion.getHeaders
import coverosR3z.templating.FileReader
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket


lateinit var clientSocket : Socket
lateinit var serverSocket : Socket
lateinit var halfOpenServerSocket : ServerSocket
lateinit var serverThread: Thread
lateinit var server : IOHolder
lateinit var client : IOHolder
lateinit var su: ServerUtilities

class ServerSocketInitializer(): Runnable {

    override fun run() {
        halfOpenServerSocket = ServerSocket(12321)
        serverSocket = halfOpenServerSocket.accept()
    }
}

class SocketTests() {

    val pmd = PureMemoryDatabase()

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
        server = IOHolder(serverSocket)
        su = ServerUtilities(server, pmd)
        client = IOHolder(clientSocket)
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

        // server - handle the request
        su.serverHandleRequest()

        // client - read status line
        val statusline = client.readLine()
        // client - read the headers
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        // client - read the body
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertEquals(1, headers.size)
        val fileWeRead = FileReader.read(pageDesired)
        assertEquals(fileWeRead, body)
    }

    /**
     * What if we ask for nothing? like, GET / HTTP/1.1
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_AskForNothing() {
        // client - send a request to the server for the page we want
        client.write("GET / HTTP/1.1\n\n")

        // server - handle the request
        su.serverHandleRequest()

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
        assertEquals(1, headers.size)
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
        su.serverHandleRequest()

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 404 NOT FOUND", statusline)
        assertEquals(1, headers.size)
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
                "GET BLAHBLAHBLAH HTTP/1.1\n",
                "GET\n",
                "BLAHBLAHBLAH HTTP/1.1\n",
                "HTTPBYRON/1.1\n",
        )
        for (request in badRequests) {
            client.write(request)

            // server - handle the request
            su.serverHandleRequest()

            // client - read status line
            val statusline = client.readLine()
            val headers = getHeaders(client)
            val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
            val body = client.read(length)

            // assert all is well
            assertEquals("HTTP/1.1 400 BAD REQUEST", statusline)
            assertEquals(1, headers.size)
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
        su.serverHandleRequest()

        // client - read status line
        val statusline = client.readLine()
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertEquals(1, headers.size)
        val fileWeRead = FileReader.read("sample.html")
        assertEquals(fileWeRead, body)
    }


    /**
     * When we POST some data, we should receive a success message back
     */
    @Test
    fun testShouldGetSuccessResponseAfterPost() {
        client.write("POST /entertime HTTP/1.1\n")
        client.write("Content-Length: 63\n\n")
        client.write("project_entry=projecta&time_entry=2&detail_entry=nothing+to+say")

        // server - handle the request
        su.serverHandleRequest()

        // client - read status line
        val statusline = client.readLine() // freezes here
        val headers = getHeaders(client)
        val length: Int = contentLengthRegex.matchEntire(headers[0])!!.groups[1]!!.value.toInt()
        val body = client.read(length)

        // assert all is well
        assertEquals("HTTP/1.1 200 OK", statusline)
        assertEquals(1, headers.size)
        assertEquals("<p>Thank you, your time has been recorded</p>", body)
    }

    @Ignore("This is to assist in manual testing only")
    @Test
    fun testingRealConnectionWithBrowser() {
        // handle the GET
        val halfOpenServerSocket = ServerSocket(8080)
        // Following line runs when we connect with the browser
        while (true) {
            val serverSocket = halfOpenServerSocket.accept()
            val server = IOHolder(serverSocket)
            val su = ServerUtilities(server, pmd)
            su.serverHandleRequest()
        }
    }




}