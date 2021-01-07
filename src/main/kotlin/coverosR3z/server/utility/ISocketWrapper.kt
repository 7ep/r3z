package coverosR3z.server.utility

interface ISocketWrapper {
    fun write(input: String)
    fun writeBytes(input: ByteArray)
    fun readLine(): String?
    fun read(len : Int) : String
    fun close()
}
