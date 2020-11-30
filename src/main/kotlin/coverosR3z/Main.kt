package coverosR3z

import coverosR3z.logging.logInfo
import coverosR3z.server.Server
import coverosR3z.server.Server.Companion.extractFirstArgumentAsPort

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    val port = extractFirstArgumentAsPort(args)
    logInfo("starting server on port $port")
    val dbDirectory = "db/"
    Server(port, dbDirectory).startServer()
}

