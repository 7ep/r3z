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
    createSystemRunningMarker()

    val serverOptions = extractCommandLineOptions(args)

    startSystem(serverOptions)
}

/**
 * Kicks off a multitude of components, including the database
 * and the server
 */
private fun startSystem(serverOptions: SystemOptions) {
    configureLogging(serverOptions)

    // start the database
    val pmd = Server.makeDatabase(dbDirectory = serverOptions.dbDirectory)

    // create the utilities that the API's will use by instantiating
    // them with the database as a parameter
    val businessObjects = Server.initializeBusinessCode(pmd)

    // create a server object
    val server = Server(serverOptions.port, serverOptions.sslPort)

    val serverExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
    val serverFuture = serverExecutor.submit(server.createServerThread(businessObjects))

    val sslServerFuture = if (serverOptions.sslPort != null) {
        val sslServerExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
        sslServerExecutor.submit(server.createSecureServerThread(businessObjects))
    } else {
        null
    }

    server.addShutdownHook(pmd, serverFuture, sslServerFuture)
}

/**
 * this saves a file to the home directory, SYSTEM_RUNNING,
 * that will indicate the system is active
 */
private fun createSystemRunningMarker() {
    File("SYSTEM_RUNNING").writeText("This file serves as a marker to indicate the system is running.")
    File("SYSTEM_RUNNING").deleteOnExit()
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

