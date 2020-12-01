package coverosR3z

import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import coverosR3z.logging.logStart
import coverosR3z.server.Server
import coverosR3z.server.Server.Companion.extractFirstArgumentAsPort

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    logSettings[LogTypes.DEBUG] = true
    logSettings[LogTypes.TRACE] = true
    val port = extractFirstArgumentAsPort(args)
    logStart("starting server on port $port")
    val dbDirectory = "db/"
    logStart("database directory is $dbDirectory")
    Server(port, dbDirectory).startServer()
}

