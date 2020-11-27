package coverosR3z

import coverosR3z.logging.logInfo
import coverosR3z.server.SocketCommunication
import coverosR3z.server.SocketCommunication.Companion.extractFirstArgumentAsPort

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    val port = extractFirstArgumentAsPort(args)
    logInfo("starting server on port $port")
    val dbDirectory = "db/"
    SocketCommunication(port, dbDirectory).startServer()
}

