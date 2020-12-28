package coverosR3z

import coverosR3z.exceptions.ServerOptionsException
import coverosR3z.logging.logImperative
import coverosR3z.server.Server
import coverosR3z.server.Server.Companion.extractOptions
import coverosR3z.server.ServerOptions
import kotlin.system.exitProcess

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {

    val serverOptions = extractCommandLineOptions(args)

    Server.addShutdownHook()
    logImperative("starting server on port ${serverOptions.port}")
    logImperative("database directory is ${serverOptions.dbDirectory}")
    Server(serverOptions.port).startServer(Server.initializeBusinessCode(dbDirectory = serverOptions.dbDirectory))

}

/**
 * The user can provide command-line options when running this.
 * See [ServerOptions] and [extractOptions]
 */
private fun extractCommandLineOptions(args: Array<String>): ServerOptions {
    return try {
        extractOptions(args)
    } catch (ex: ServerOptionsException) {
        println(ex.message)
        exitProcess(0)
    }
}

