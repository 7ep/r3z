package coverosR3z.server.utility

import java.net.Socket

interface ISocketWrapper {
    val socket: Socket
    val name: String?
    fun write(input: String)
    fun writeBytes(input: ByteArray)
    fun readLine(): String?
    fun read(len : Int) : String
    fun close()
}
