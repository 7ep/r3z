package coverosR3z.server

import coverosR3z.templating.FileReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.net.Socket

/**
 * This is our regex for looking at a client's request
 * and determining what to send them.  For example,
 * if they send GET /sample.html HTTP/1.1, we send them sample.html
 *
 * On the other hand if it's not a well formed request, or
 * if we don't have that file, we reply with an error page
 */
val pageExtractorRegex = "GET /(.*) HTTP/1.1".toRegex()

/**
 * Provides access to the reading and writing functions on a socket
 * in a standardized, tightly-controlled way
 */
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

/**
 * Code on the server that will handle a request from a
 * client.  This is hardcoded to handle just one thing:
 * GET / HTTP/1.1
 */
fun serverHandleRequest(server: IOHolder) {
    // read a line
    val serverInput = server.readLine()

    // check that we got a valid client request
    // if we didn't get a good result from looking at the client's
    // request, something is horribly wrong - return a 400
    // result == null means we didn't match anything (I think)
    val result: MatchResult? = pageExtractorRegex.matchEntire(serverInput)
    if (result == null) {
        handleInvalidRequest(server)
    } else {
        try {
            // read the file requested by the client
            val fileRequested = result.groups[1]!!.value
            returnFile(fileRequested, server)
        } catch (ex : IllegalArgumentException) {
            handleUnfound(server)
        }
    }

}

/**
 * If we are asked for something we don't have, like
 * if they request GET /foo HTTP/1.1 and we don't have foo
 */
private fun handleUnfound(server: IOHolder) {
    // prepare some data to send from the server
    val status = "HTTP/1.1 404 NOT FOUND"
    val fileWeRead = FileReader.read("404error.html")
    val header = "Content-Length: ${fileWeRead.length}"
    val input = "$status\n" +
            "$header\n" +
            "\n" +
            fileWeRead
    server.write(input)
}

/**
 * Return the requested file, if we have it, from a
 * request like GET /file HTTP/1.1
 */
private fun returnFile(fileRequested: String, server: IOHolder) {
    // server - send a page to the client
    // prepare some data to send from the server
    val status = "HTTP/1.1 200 OK"
    val fileWeRead = FileReader.read(fileRequested)
    val header = "Content-Length: ${fileWeRead.length}"
    val input = "$status\n" +
            "$header\n" +
            "\n" +
            fileWeRead
    server.write(input)
}

/**
 * If we are sent an invalid request,
 * like BLAH BLAH BLAH BLAH
 * instead of GET / HTTP/1.1
 */
private fun handleInvalidRequest(server: IOHolder) {
    // prepare some data to send from the server
    val status = "HTTP/1.1 400 BAD REQUEST"
    val fileWeRead = FileReader.read("400error.html")
    val header = "Content-Length: ${fileWeRead.length}"
    val input = "$status\n" +
            "$header\n" +
            "\n" +
            fileWeRead
    server.write(input)
}