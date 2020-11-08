package coverosR3z.server

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

/**
 * Provides access to the reading and writing functions on a socket
 * in a standardized, tightly-controlled way
 */
class SocketWrapper(socket: Socket) : ISocketWrapper {
    private val writer: OutputStream = socket.getOutputStream()
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.inputStream))

    override fun write(input: String) {
        writer.write(input.toByteArray())
    }

    override fun readLine(): String {
        return reader.readLine()
    }

    override fun read(len : Int) : String {
        val cbuf = CharArray(len)
        reader.read(cbuf, 0, len)
        return cbuf.joinToString("")
    }
}