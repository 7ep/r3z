package coverosR3z.server

import coverosR3z.logging.logDebug
import coverosR3z.logging.logTrace
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.util.*

/**
 * Provides access to the reading and writing functions on a socket
 * in a standardized, tightly-controlled way
 */
class SocketWrapper(val socket: Socket, val name : String? = null) : ISocketWrapper {
    private val writer: OutputStream = socket.getOutputStream()
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.inputStream))

    override fun write(input: String) {
        logTrace("${name ?: socket} is sending $input")
        writer.write(input.toByteArray())
    }

    override fun writeBytes(input: ByteArray) {
        writer.write(input)
    }

    override fun readLine(): String {
        val readResult = reader.readLine() ?: ""
        logTrace("${name ?: socket} read this line: $readResult")
        return readResult
    }

    override fun read(len : Int) : String {
        val buf = CharArray(len)
        val lengthRead = reader.read(buf, 0, len)
        val body = buf.slice(0 until lengthRead).joinToString("")
        logDebug("$name actually read $lengthRead bytes.  body: $body")
        return body
    }

    override fun close() {
        socket.close()
    }

}