package coverosR3z.server

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

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