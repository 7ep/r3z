package coverosR3z.server.utility

import coverosR3z.FullSystem
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.ServerObjects
import java.net.ServerSocket
import java.util.concurrent.ExecutorService


/**
 * This is the top-level entry into the web server
 * @param port the port the server will run on
 */
class Server(
    val port: Int,
    private val executorService: ExecutorService,
    private val businessObjects: BusinessCode,
    private val serverObjects: ServerObjects,
    private val fullSystem: FullSystem
) {

    // create the regular (non-secure) socket
    val halfOpenServerSocket = ServerSocket(port)

    init {
        logImperative("System is ready at http://localhost:$port")
    }

    fun createServerThread() : Thread {
        return ServerUtilities.createServerThread(executorService, fullSystem, halfOpenServerSocket, businessObjects, serverObjects)
    }

}