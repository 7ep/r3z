package coverosR3z.server

data class ServerOptions(
        /**
         * The port the server will use
         */
        val port : Int = 12345,
        /**
         * the directory in which we will store the data
         * if this is set to null, we won't store anything to disk,
         * which is really just useful for testing
         */
        val dbDirectory : String? = "db")