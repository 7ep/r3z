package coverosR3z.server

import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.logging.Logger
import coverosR3z.logging.logInfo
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import java.net.ServerSocket
import java.util.concurrent.Executors

/**
 * This is the top-level class that handles communication with clients.
 * The intention is that this class solely acts to control the socket
 * connections and is unfamiliar with the specifics of what is being transmitted
 */
class Server(val port : Int, val dbDirectory: String) {

    private lateinit var halfOpenServerSocket : ServerSocket

    fun startServer() {
        halfOpenServerSocket = ServerSocket(port)
        val cu = CurrentUser(SYSTEM_USER)
        val pmd = PureMemoryDatabase.start(dbDirectory)
        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), cu)
        val au = AuthenticationUtilities(AuthenticationPersistence(pmd))
        logInfo("System is ready")

        val cachedThreadPool = Executors.newCachedThreadPool()
        while (shouldContinue) {
            logInfo("waiting for socket connection")
            val server = SocketWrapper(halfOpenServerSocket.accept(), "server")
            val thread = Thread {
                logInfo("client from ${server.socket.inetAddress?.hostAddress} has connected")
                handleRequest(server, au, tru)
                server.close()
            }
            cachedThreadPool.submit(thread)
        }
        halfOpenServerSocket.close()
    }

    companion object {

        var shouldContinue = true

        /**
         * Given the command-line arguments, returns the first value
         * as an integer for use as a port number, or defaults to 8080
         */
        fun extractFirstArgumentAsPort(args: Array<String>): Int {
            return if (args.isEmpty() || args[0].isBlank()) {
                8080
            } else {
                args[0].toIntOrNull() ?: 8080
            }
        }

        fun handleRequest(server: ISocketWrapper, au: IAuthenticationUtilities, tru: ITimeRecordingUtilities) {
            val responseData = try {
                val requestData = parseClientRequest(server, au)

                // now that we know who the user is (if they authenticated) we can update the current user
                val cu = CurrentUser(requestData.user)
                val truWithUser = tru.changeUser(cu)
                Logger(cu).info("client requested ${requestData.verb} ${requestData.path}")
                handleRequestAndRespond(ServerData(au, truWithUser, requestData))
            } catch (ex : Exception) {
                okHTML(generalMessageHTML(ex.message ?: "NO ERROR MESSAGE DEFINED"))
            }

            returnData(server, responseData)

        }
    }

}