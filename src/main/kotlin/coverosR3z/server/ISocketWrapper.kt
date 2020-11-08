package coverosR3z.server

interface ISocketWrapper {
    fun write(input: String)
    fun readLine(): String
    fun read(len : Int) : String
}
