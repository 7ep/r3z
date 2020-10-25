package coverosR3z.server

import coverosR3z.templating.FileReader
import org.junit.AfterClass
import org.junit.Assert.assertEquals
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
lateinit var server : IOHolder
lateinit var client : IOHolder

val contentLengthRegex = "Content-Length: (.*)$".toRegex()

class ServerSocketInitializer(): Runnable {

    override fun run() {
        halfOpenServerSocket = ServerSocket(12321)
        serverSocket = halfOpenServerSocket.accept()
    }
}

class SocketTests() {

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
            clientSocket.close()
            serverSocket.close()
        }
    }

    @Before
    fun init() {
        server = IOHolder(serverSocket)
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

        assert(response.contains("<!DOCTYPE html>"))
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
        client.write("GET /$pageDesired HTTP/1.1\n")

        // server - handle the request
        serverHandleRequest(server)

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
        assertEquals("Content-Length: 853", headers[0])
        val fileWeRead = FileReader.read(pageDesired)
        assertEquals(fileWeRead, body)
    }

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        // send a request to the server for something that doesn't exist
        client.write("GET /doesnotexist HTTP/1.1\n")

        // server - handle the request
        serverHandleRequest(server)

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
        client.write("GET BLAHBLAHBLAH HTTP/1.1\n")

        // server - handle the request
        serverHandleRequest(server)

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

    /**
     * Helper method to collect the headers line by line, stopping when it
     * encounters an empty line
     */
    private fun getHeaders(client: IOHolder): MutableList<String> {
        // get the headers
        val headers = mutableListOf<String>()
        while (true) {
            val header = client.readLine()
            if (header.length > 0) {
                headers.add(header)
            } else {
                break
            }
        }
        return headers
    }


}