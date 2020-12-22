package coverosR3z.server

import coverosR3z.exceptions.ServerOptionsException
import coverosR3z.misc.checkParseToInt

data class ServerOptions(
        /**
         * The port the server will use
         */
        val port : Int = defaultPort,
        /**
         * the directory in which we will store the data
         * if this is set to null, we won't store anything to disk,
         * which is really just useful for testing
         */
        val dbDirectory : String? = "db/"){
        companion object{
                const val defaultPort = 12345
                fun make(port : String, dbDirectory : String, ndp : String) : ServerOptions{
                        val makePort = if (port.isNotBlank()) checkParseToInt(port) else defaultPort
                        check(!(dbDirectory.isNotBlank() && ndp=="true")){"If you're setting the noDiskPersistence option and also a database directory, you're very foolish."}
                        val makeDBDir : String? =
                                when {
                                        ndp == "true" -> null
                                        dbDirectory.isNotBlank() -> if (! dbDirectory.endsWith("/")) "$dbDirectory/" else dbDirectory
                                        else -> "db/"
                                }

                        if (makePort < 1 || makePort > 65535) {throw ServerOptionsException("port number was out of range.  Range is 1-65535.  Your input was: $makePort")}
                        return ServerOptions(makePort, makeDBDir)
                }
        }
}