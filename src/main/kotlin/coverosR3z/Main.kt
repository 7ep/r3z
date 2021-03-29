package coverosR3z

import coverosR3z.system.utility.FullSystem.Companion.createSystemRunningMarker
import coverosR3z.system.utility.FullSystem.Companion.startSystem
import coverosR3z.config.utility.SystemOptions.Companion.extractCommandLineOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Entry point for the application.  KISS.
 */
fun main(args: Array<String>) {
    // write a file that exists while the server is running,
    // making it clearer that the program is operational
    createSystemRunningMarker()

    // get the command-line options from the user, if any
    val systemOptions = extractCommandLineOptions(args)

    // create an executor service for all the threads in the system
    val esForThreadsInServer: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
    val systemFuture: Future<*> = esForThreadsInServer.submit {startSystem(systemOptions, esForThreadsInServer = esForThreadsInServer)}

    // block here for threads to complete
    systemFuture.get()
}


