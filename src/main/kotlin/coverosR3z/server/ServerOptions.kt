package coverosR3z.server

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
        val dbDirectory : String? = "db"){
        companion object{
                const val defaultPort = 12345
                fun make(port : Int?, dbDirectory : String?, ndp : Boolean?) : ServerOptions{
                        val makePort = port ?: defaultPort
                        check(!(dbDirectory!=null && ndp==true)){"If you're setting the noDiskPersistence option and also a database directory, you're very foolish."}
                        val makeDBDir = if(ndp==true) null else dbDirectory
                        return ServerOptions(makePort, makeDBDir)
                }
        }
}