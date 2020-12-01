package coverosR3z.server

import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.domainobjects.DateTime
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.logging.logDebug
import coverosR3z.logging.logStart
import coverosR3z.logging.logTrace
import coverosR3z.logging.systemStartMillis
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

/**
 * This is the top-level entry into the web server
 */
class Server(val port: Int, val dbDirectory: String) {

    lateinit var halfOpenServerSocket : ServerSocket

    fun startServer() {
        halfOpenServerSocket = ServerSocket(port)
        val cu = CurrentUser(SYSTEM_USER)
        val pmd = PureMemoryDatabase.start(dbDirectory)
        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), cu)
        val au = AuthenticationUtilities(AuthenticationPersistence(pmd))
        logStart("System is ready.  DateTime is ${DateTime(systemStartMillis / 1000)} in UTC.  Subsequent entries are offset from this time, in milliseconds")

        val numCores = Runtime.getRuntime().availableProcessors()
        logStart("Found $numCores cores.  Starting that many threads")
        val cachedThreadPool = Executors.newFixedThreadPool(numCores)
        while (shouldContinue) {
            logDebug("waiting for socket connection")
            val server = SocketWrapper(halfOpenServerSocket.accept(), "server")
            if (!shouldContinue) {break}
            val thread = Thread {
                logDebug("client from ${server.socket.inetAddress?.hostAddress} has connected")
                do {
                    val requestData = handleRequest(server, au, tru)
                    val shouldKeepAlive =  requestData.headers.any{it.toLowerCase().contains("connection: keep-alive")}
                    if (shouldKeepAlive) {
                        logTrace("Since this is a keep-alive connection, setting a 1-second timeout on read")
                        server.socket.soTimeout = 1000
                    }
                } while(shouldContinue && shouldKeepAlive)

                logTrace("closing server socket")
                server.close()
            }
            cachedThreadPool.submit(thread)
        }
        logTrace("Closing the primary server socket")
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

        fun handleRequest(server: ISocketWrapper, au: IAuthenticationUtilities, tru: ITimeRecordingUtilities) : RequestData {
            lateinit var requestData : RequestData
            val responseData: PreparedResponseData = try {
                requestData = parseClientRequest(server, au)
                // now that we know who the user is (if they authenticated) we can update the current user
                val cu = CurrentUser(requestData.user)
                val truWithUser = tru.changeUser(cu)
                logDebug("client requested ${requestData.verb} ${requestData.path}", cu)
                handleRequestAndRespond(ServerData(au, truWithUser, requestData))
            } catch (ex : SocketTimeoutException) {
                logTrace("Socket timed out: ${ex.message}")
                return requestData
            } catch (ex: Exception) {
                handleInternalServerError(ex.message ?: "NO ERROR MESSAGE DEFINED")
            }

            returnData(server, responseData)
            return requestData
        }
    }

}