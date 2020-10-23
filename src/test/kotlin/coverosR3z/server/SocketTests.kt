package coverosR3z.server

import coverosR3z.FileReader
import org.junit.*
import org.junit.Assert.assertEquals
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket


var clientSocket : Socket = Socket("localhost", 12321)
var serverSocket : Socket = Socket("localhost", 12321)

class ServerSocketInitializer(): Runnable {

    override fun run() {
        var halfOpenServerSocket = ServerSocket(12321)
        serverSocket = halfOpenServerSocket.accept()
    }
}

class SocketTests() {

    companion object{
        @BeforeClass @JvmStatic
        fun openSockets() {
            val server = Thread(ServerSocketInitializer())
            server.start()
            clientSocket = Socket("localhost", 12321)
        }

        @AfterClass @JvmStatic
        fun closeSockets() {
            serverSocket.close()
            clientSocket.close()
        }
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
        //set up server and client input streams
        val clientReader = BufferedReader(InputStreamReader(clientSocket.inputStream))
        val serverReader = BufferedReader(InputStreamReader(serverSocket.inputStream))
        val clientWriter = clientSocket.getOutputStream()
        val serverWriter = serverSocket.getOutputStream()

        clientWriter.write("GET / HTTP/1.1\n".toByteArray())
        val serverInput = serverReader.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            serverWriter.write("HTTP/1.1 200 OK\n".toByteArray())
        }
        val response = clientReader.readLine()

        assertEquals(response, "HTTP/1.1 200 OK")
    }

    @Test
    fun `I want to hit a homepage when I point my browser to the application domain`(){
        val clientReader = BufferedReader(InputStreamReader(clientSocket.inputStream))
        val serverReader = BufferedReader(InputStreamReader(serverSocket.inputStream))
        val clientWriter = clientSocket.getOutputStream()
        val serverWriter = serverSocket.getOutputStream()

        clientWriter.write("GET / HTTP/1.1\n".toByteArray())
        val serverInput = serverReader.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            serverWriter.write("HTTP/1.1 200 OK\n <!DOCTYPE html>\n (the whole text of the page)\n".toByteArray())
        }

        clientReader.readLine()
        val response = clientReader.readLine()

        assert(response.contains("<!DOCTYPE html>"))
    }

    @Test
    fun `should see a welcome message from an html file when GET requesting server`() {
        val clientReader = BufferedReader(InputStreamReader(clientSocket.inputStream))
        val serverReader = BufferedReader(InputStreamReader(serverSocket.inputStream))
        val clientWriter = clientSocket.getOutputStream()
        val serverWriter = serverSocket.getOutputStream()

        val webpage = FileReader.read("sample.html")
        clientWriter.write("GET / HTTP/1.1\n".toByteArray())
        val serverInput = serverReader.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            serverWriter.write("HTTP/1.1 200 OK\n <!DOCTYPE html>\n${webpage}\n".toByteArray())
        }

        repeat(2) {clientReader.readLine()}
        var body = ""
        var nextLine = clientReader.readLine()
        while (nextLine != "</html>") {
            body += "${nextLine}\n"
            nextLine = clientReader.readLine()
        }
        body += nextLine
        assertEquals(webpage, body)
    }
}