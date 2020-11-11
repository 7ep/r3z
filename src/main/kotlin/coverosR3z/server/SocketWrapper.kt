package coverosR3z.server

import coverosR3z.logging.logDebug
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

/**
 * Provides access to the reading and writing functions on a socket
 * in a standardized, tightly-controlled way
 */
class SocketWrapper(val socket: Socket) : ISocketWrapper {
    private val writer: OutputStream = socket.getOutputStream()
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.inputStream))

    override fun write(input: String) {
        logDebug("$socket is sending $input")
        writer.write(input.toByteArray())
    }

    override fun readLine(): String {
        val readResult = reader.readLine()
        logDebug("$socket read this line: $readResult")
        return readResult
    }

    override fun read(len : Int) : String {
        val cbuf = CharArray(len)
        reader.read(cbuf, 0, len)
        logDebug("$socket read $len bytes of this: ${cbuf.joinToString("")}")
        return cbuf.joinToString("")
    }
}