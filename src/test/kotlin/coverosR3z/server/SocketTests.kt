package coverosR3z.server

import coverosR3z.FileReader
import org.junit.*
import org.junit.Assert.assertEquals
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

var clientSocket : Socket = Socket("localhost", 12321)
var serverSocket : Socket = Socket("localhost", 12321)
lateinit var clientReader: BufferedReader
lateinit var serverReader: BufferedReader
lateinit var clientWriter: OutputStream
lateinit var serverWriter: OutputStream

class SocketTests() {

    class ServerSocketInitializer(): Runnable {

        override fun run() {
            var halfOpenServerSocket = ServerSocket(12321)
            serverSocket = halfOpenServerSocket.accept()

            //set up server and client input streams
            clientReader = BufferedReader(InputStreamReader(clientSocket.inputStream))
            serverReader = BufferedReader(InputStreamReader(serverSocket.inputStream))
            clientWriter = clientSocket.getOutputStream()
            serverWriter = serverSocket.getOutputStream()
        }
    }

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
        clientWriter.write("GET / HTTP/1.1\n".toByteArray())
        val serverInput = serverReader.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            serverWriter.write("HTTP/1.1 200 OK\n".toByteArray())
        }
        val response = clientReader.readLine()

        assertEquals(response, "HTTP/1.1 200 OK")
    }

    @Test
    fun testShouldGetHtmlResponseFromServer(){
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
    fun testShouldGetHtmlFileResponseFromServer() {
        val webpage = FileReader.read("sample.html")
        clientWriter.write("GET / HTTP/1.1\n".toByteArray())
        val serverInput = serverReader.readLine()
        if(serverInput == "GET / HTTP/1.1"){
            serverWriter.write("HTTP/1.1 200 OK\n <!DOCTYPE html>\n${webpage}\n".toByteArray())
        }

        repeat(1) {clientReader.readLine()}
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