package coverosR3z

import coverosR3z.config.utility.SystemOptions
import coverosR3z.logging.configureLogging
import coverosR3z.server.utility.Server
import java.io.File
import java.util.concurrent.Executors

/**
 * This serves as a central location for the code
 * needed to start the system, called by Main
 */
class Ignition {

    companion object {

        /**
         * Kicks off a multitude of components, including the database
         * and the server
         */
        fun startSystem(serverOptions: SystemOptions) {
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
        fun createSystemRunningMarker() {
            File("SYSTEM_RUNNING").writeText("This file serves as a marker to indicate the system is running.")
            File("SYSTEM_RUNNING").deleteOnExit()
        }

    }
}