package coverosR3z.server

import coverosR3z.templating.FileReader
import coverosR3z.templating.TemplatingEngine
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.net.Socket

data class PreparedResponseData(val fileContents: String, val server: IOHolder, val code: String, val status: String)

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
 * Serve prepared response object to the client
 */
fun serverHandleRequest(server: IOHolder) {
    returnData(prepareResponseData(server))
}

/**
 * Code on the server that will handle a request from a
 * client.  This is hardcoded to handle just one thing:
 * GET / HTTP/1.1
 * Prepares a response object to be served to the client
 */
fun prepareResponseData(server: IOHolder): PreparedResponseData {
    // read a line
    val serverInput = server.readLine()
    val result: MatchResult = pageExtractorRegex.matchEntire(serverInput)
            ?: return PreparedResponseData(FileReader.read("400error.html"), server, "400", "BAD REQUEST")

    // if the server request doesn't match our regex, it's invalid
    // get the file requested
    val requestedFileMatch = checkNotNull(result.groups[1])

    val fileToRead = requestedFileMatch.value

    val isFound: Boolean = FileReader.exists(fileToRead)

    if (!isFound) {
        return PreparedResponseData(FileReader.read("404error.html"), server, "404", "NOT FOUND")
    }

    val file = if (fileToRead.takeLast(4) == ".utl") {
        renderTemplate(fileToRead)
    } else {
        FileReader.read(fileToRead)
    }

    return PreparedResponseData(file, server, "200", "OK")
}

private fun renderTemplate(requestedFile: String): String {
    val template = FileReader.read(requestedFile)
    val te = TemplatingEngine()
    // TODO: replace following code ASAP
    val mapping = mapOf("username" to "Jona")
    return te.render(template, mapping)
}

/**
 * sends data as the body of a response from server
 */
private fun returnData(data: PreparedResponseData) {

    val status = "HTTP/1.1 ${data.code} ${data.status}"
    val header = "Content-Length: ${data.fileContents.length}"
    val input = "$status\n" +
            "$header\n" +
            "\n" +
            data.fileContents
    data.server.write(input)
}
