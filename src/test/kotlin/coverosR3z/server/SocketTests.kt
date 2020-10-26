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

class ServerSocketInitializer(): Runnable {

    override fun run() {
        val halfOpenServerSocket = ServerSocket(12321)
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
        val server = IOHolder(serverSocket)
        val client = IOHolder(clientSocket)

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
        val server = IOHolder(serverSocket)
        val client = IOHolder(clientSocket)

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
        val server = IOHolder(serverSocket)
        val client = IOHolder(clientSocket)

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

    @Test
    fun testShouldGetHtmlFileResponseFromServer_MaybeRefactored() {
        val server = IOHolder(serverSocket)
        val client = IOHolder(clientSocket)

        val fileWeRead = FileReader.read("sample.html")
        client.write("GET / HTTP/1.1\n")

        val serverInput = server.readLine()

        val status = "HTTP/1.1 200 OK"
        val header = "Content-Length: ${fileWeRead.length}"
        val input = "$status\n" +
                "$header\n" +
                "\n" +
                "${fileWeRead}"

        if(serverInput == "GET / HTTP/1.1"){
            server.write(input)
        }

        var responseFromServer = client.read(input.length).substring("$status\n$header\n\n".length)

        assertEquals(fileWeRead, responseFromServer)
    }

    class IOHolder(socket: Socket) {
        private val writer: OutputStream = socket.getOutputStream()
        private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.inputStream))

        fun write(input: String) {
            writer.write(input.toByteArray())
        }

        fun readLine(): String {
            return reader.readLine()
        }

        fun read(len : Int) : String {
            val cbuf = CharArray(len)
            reader.read(cbuf, 0, len)
            return cbuf.joinToString("")
        }
    }

}