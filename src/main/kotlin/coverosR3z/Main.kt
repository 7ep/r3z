package coverosR3z

import coverosR3z.misc.exceptions.SystemOptionsException
import coverosR3z.logging.logImperative
import coverosR3z.config.utility.SystemOptions
import coverosR3z.server.utility.Server
import coverosR3z.config.utility.SystemOptions.Companion.extractOptions
import coverosR3z.logging.turnOffAllLogging
import coverosR3z.logging.turnOnAllLogging
import java.io.File
import java.util.concurrent.Executors
import kotlin.system.exitProcess

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    File("SYSTEM_RUNNING").writeText("This file serves as a marker to indicate the system is running.")
    File("SYSTEM_RUNNING").deleteOnExit()

    val serverOptions = extractCommandLineOptions(args)

    logImperative("starting server on port ${serverOptions.port} and sslPort ${serverOptions.sslPort}")
    logImperative("database directory is ${serverOptions.dbDirectory}")

    configureLogging(serverOptions)

    val pmd = Server.makeDatabase(dbDirectory = serverOptions.dbDirectory)
    val businessObjects = Server.initializeBusinessCode(pmd)
    val server = Server(serverOptions.port, serverOptions.sslPort)
    val serverExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
    val serverFuture = serverExecutor.submit(server.createServerThread(businessObjects))

    if (serverOptions.sslPort != null) {
        val sslServerExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
        val sslServerFuture = sslServerExecutor.submit(server.createSecureServerThread(businessObjects))
        server.addShutdownHook(pmd, serverFuture, sslServerFuture)
    } else {
        server.addShutdownHook(pmd, serverFuture)
    }
}

/**
 * Sets logging per the [SystemOptions], if the user requested
 * something
 */
private fun configureLogging(serverOptions: SystemOptions) {
    if (serverOptions.allLoggingOff) {
        turnOffAllLogging()
    }
    if (serverOptions.allLoggingOn) {
        turnOnAllLogging()
    }
}

/**
 * The user can provide command-line options when running this.
 * See [SystemOptions] and [extractOptions]
 */
private fun extractCommandLineOptions(args: Array<String>): SystemOptions {
    return try {
        extractOptions(args)
    } catch (ex: SystemOptionsException) {
        println(ex.message)
        exitProcess(0)
    }
}

