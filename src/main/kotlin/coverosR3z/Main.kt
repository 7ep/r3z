package coverosR3z

import coverosR3z.exceptions.ServerOptionsException
import coverosR3z.logging.logStart
import coverosR3z.server.Server
import coverosR3z.server.Server.Companion.extractOptions

/**
 * Entry point for the application.  KISS.
 * argument is the port
 */
fun main(args: Array<String>) {
    try {
        val serverOptions = extractOptions(args)
        logStart("starting server on port ${serverOptions.port}")
        logStart("database directory is ${serverOptions.dbDirectory}")
        Server(serverOptions.port, serverOptions.dbDirectory).startServer()
    } catch (ex : ServerOptionsException) {
        println(ex.message)
    }
}

