package coverosR3z.server.utility

import coverosR3z.FullSystem
import coverosR3z.logging.*
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.misc.types.DateTime
import coverosR3z.server.types.*
import coverosR3z.server.utility.ServerUtilities.Companion.processConnectedClient
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.ExecutorService


/**
 * This is the top-level entry into the web server
 * @param port the port the server will run on
 */
class Server(
    val port: Int,
    private val executorService: ExecutorService,
    private val serverObjects: ServerObjects,
    private val fullSystem: FullSystem
) {

    // create the regular (non-secure) socket
    val halfOpenServerSocket = ServerSocket(port)

    init {
        logImperative("System is ready at http://localhost:$port.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")
    }

    fun createServerThread(businessObjects : BusinessCode) : Thread {
        return Thread {
            try {
                while (true) {
                    fullSystem.logger.logTrace { "waiting for socket connection" }
                    val server = SocketWrapper(halfOpenServerSocket.accept(), "server", fullSystem)
                    executorService.submit(Thread { processConnectedClient(server, businessObjects, serverObjects) })
                }
            } catch (ex: SocketException) {
                if (ex.message == "Interrupted function call: accept failed") {
                    logImperative("Server was shutdown while waiting on accept")
                }
            }
        }
    }

}