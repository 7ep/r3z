package coverosR3z.server.utility

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.types.DateTime
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.logging.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.api.handleInternalServerError
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.Verb
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/**
 * This is the top-level entry into the web server
 * @param port the port the server will run on
 */
class Server(val port: Int) {


    lateinit var halfOpenServerSocket : ServerSocket
    private lateinit var cachedThreadPool: ExecutorService
    var systemReady = false

    /**
     * This requires a [BusinessCode] object, see [initializeBusinessCode]
     * for the typical way to create this.
     */
    fun startServer(businessObjects : BusinessCode) : Thread {
        return thread {
            halfOpenServerSocket = ServerSocket(port)
            loadStaticFilesToCache()
            try {
                cachedThreadPool = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
                systemReady = true
                logImperative("System is ready.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")
                while (true) {
                    logTrace { "waiting for socket connection" }
                    val server = SocketWrapper(halfOpenServerSocket.accept(), "server")
                    cachedThreadPool.submit(Thread { processConnectedClient(server, businessObjects) })
                }
            } catch (ex: SocketException) {
                if (ex.message == "Interrupted function call: accept failed") {
                    logImperative("Server was shutdown while waiting on accept")
                    systemReady = false
                }
            }
        }
    }

    private fun processConnectedClient(
        server: SocketWrapper,
        businessCode: BusinessCode
    ) {
        logTrace { "client from ${server.socket.inetAddress?.hostAddress} has connected" }
        do {
            val requestData = handleRequest(server, businessCode)
            val shouldKeepAlive = requestData.headers.any { it.toLowerCase().contains("connection: keep-alive") }
            if (shouldKeepAlive) {
                logTrace { "This is a keep-alive connection" }
            }
        } while (shouldKeepAlive)

        logTrace { "closing server socket" }
        server.close()
    }

    /**
     * this adds a hook to the Java runtime, so that if the app is running
     * and a user stops it - by pressing ctrl+c or a unix "kill" command - the
     * server socket will be shutdown and some messages about closing the server
     * will log
     */
    fun addShutdownHook(pmd: PureMemoryDatabase, serverThread: Thread) : Server {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                serverShutdown(pmd, serverThread)
            })
        return this
    }

    private fun serverShutdown(pmd: PureMemoryDatabase, serverThread: Thread) {
        logImperative("Received shutdown command")
        logImperative("Shutting down main server thread")
        cachedThreadPool.shutdown()
        cachedThreadPool.awaitTermination(10, TimeUnit.SECONDS)
        halfOpenServerSocket.close()

        logImperative("Shutting down logging")
        loggerPrinter.stop()

        logImperative("Shutting down the database")
        pmd.stop()

        logImperative("Waiting for the main server thread")
        serverThread.join()

        logImperative("Goodbye world!")
    }


    companion object {


        /**
         * Set up the classes necessary for business-related actions, like
         * recording time, and so on
         */
        fun initializeBusinessCode(
            pmd : PureMemoryDatabase
        ): BusinessCode {
            val cu = CurrentUser(SYSTEM_USER)
            val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), cu)
            val au = AuthenticationUtilities(AuthenticationPersistence(pmd))
            return BusinessCode(tru, au)
        }

        /**
         * Initializes the database
         * @param pmd typically you would provide null here, but you can enter a value if you want to inject a mock.  If you
         *            provide a mock, this function will ignore the dbDirectory parameter.
         * @param dbDirectory the database directory.  If you provide a string the system will use that as the directory
         *                    for the disk persistence.  If you provide null then the system will operate in memory-only,
         *                    see PureMemoryDatabase.startMemoryOnly
         */
        fun makeDatabase(
            pmd: PureMemoryDatabase? = null,
            dbDirectory: String? = null
        ): PureMemoryDatabase {
            return pmd ?: if (dbDirectory == null) {
                PureMemoryDatabase.startMemoryOnly()
            } else {
                PureMemoryDatabase.startWithDiskPersistence(dbDirectory)
            }
        }

        fun handleRequest(server: ISocketWrapper, businessCode: BusinessCode) : AnalyzedHttpData {
            lateinit var analyzedHttpData : AnalyzedHttpData
            val responseData: PreparedResponseData = try {
                analyzedHttpData = parseHttpMessage(server, businessCode.au)
                if (analyzedHttpData.verb == Verb.CLIENT_CLOSED_CONNECTION) {
                    return analyzedHttpData
                }
                // now that we know who the user is (if they authenticated) we can update the current user
                val cu = CurrentUser(analyzedHttpData.user)
                val truWithUser = businessCode.tru.changeUser(cu)
                logDebug(cu) { "client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}" }
                directToProcessor(ServerData(businessCode.au, truWithUser, analyzedHttpData))
            } catch (ex: Exception) {
                handleInternalServerError(ex.message ?: ex.stackTraceToString(), ex.stackTraceToString())
            }

            returnData(server, responseData)
            return analyzedHttpData
        }
    }

}