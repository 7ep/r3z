package coverosR3z.server.utility

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.types.DateTime
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.logging.*
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.api.handleBadRequest
import coverosR3z.server.api.handleInternalServerError
import coverosR3z.server.types.*
import coverosR3z.server.utility.AuthUtilities.Companion.isAuthenticated
import coverosR3z.server.utility.RoutingUtilities.Companion.routeToEndpoint
import coverosR3z.server.utility.ServerUtilities.Companion.loadStaticFilesToCache
import coverosR3z.server.utility.ServerUtilities.Companion.returnData
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory


/**
 * This is the top-level entry into the web server
 * @param port the port the server will run on
 * @param sslPort the secure port
 */
class Server(val port: Int, private val sslPort: Int? = null) {

    // create the non-secure socket
    val halfOpenServerSocket = ServerSocket(port)

    // create the secure SSL (TLS 1.3) socket
    private lateinit var sslHalfOpenServerSocket : SSLServerSocket

    private val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
    var systemReady = false

    init {
        // set up the secure server socket, if we were given a port
        if (sslPort != null) {
            sslHalfOpenServerSocket = SSLServerSocketFactory.getDefault().createServerSocket(sslPort) as SSLServerSocket
            sslHalfOpenServerSocket.enabledProtocols = arrayOf("TLSv1.3")
            sslHalfOpenServerSocket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
        }

        // get the static files like CSS, JS, etc loaded into a cache
        loadStaticFilesToCache(staticFileCache)

        systemReady = true
        val extraSslText =  if (sslPort != null) " and https://localhost:$sslPort" else ""
        logImperative("System is ready at http://localhost:$port$extraSslText.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")
    }

    /**
     * This requires a [BusinessCode] object, see [initializeBusinessCode]
     * for the typical way to create this.
     */
    fun createServerThread(businessObjects : BusinessCode) : Thread {
        return Thread {
            try {
                while (true) {
                    logTrace { "waiting for socket connection" }
                    val server = SocketWrapper(halfOpenServerSocket.accept(), "server")
                    cachedThreadPool.submit(Thread { processConnectedClient(server, businessObjects) })
                }
            } catch (ex: SocketException) {
                if (ex.message == "Interrupted function call: accept failed") {
                    logImperative("Server was shutdown while waiting on accept")
                }
            }
        }
    }

    /**
     * This requires a [BusinessCode] object, see [initializeBusinessCode]
     * for the typical way to create this.
     */
    fun createSecureServerThread(businessObjects : BusinessCode) : Thread {
        return Thread {
            try {
                while (true) {
                    logTrace { "waiting for socket connection" }
                    val server = SocketWrapper(sslHalfOpenServerSocket.accept(), "server")
                    cachedThreadPool.submit(Thread { processConnectedClient(server, businessObjects) })
                }
            } catch (ex: SocketException) {
                if (ex.message == "Interrupted function call: accept failed") {
                    logImperative("Server was shutdown while waiting on accept")
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
    fun addShutdownHook(pmd: PureMemoryDatabase, serverThread: Future<*>, sslServerFuture: Future<*>? = null) : Server {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                serverShutdown(pmd, serverThread, sslServerFuture)
            })
        return this
    }

    private fun serverShutdown(pmd: PureMemoryDatabase, serverFuture: Future<*>, sslServerFuture: Future<*>? = null) {
        logImperative("Received shutdown command")
        logImperative("Shutting down main server thread")
        cachedThreadPool.shutdown()
        cachedThreadPool.awaitTermination(10, TimeUnit.SECONDS)
        halfOpenServerSocket.close()
        if (sslPort != null) {
            sslHalfOpenServerSocket.close()
        }

        logImperative("Shutting down logging")
        loggerPrinter.stop()

        logImperative("Shutting down the database")
        pmd.stop()

        logImperative("Waiting for the non-ssl server thread")
        serverFuture.get()

        if (sslServerFuture != null) {
            logImperative("Waiting for the ssl server thread")
            sslServerFuture.get()
        }

        systemReady = false
        logImperative("Goodbye world!")
    }


    companion object {

        /**
         * A simple cache for the static files.
         */
        val staticFileCache = mutableMapOf<String, PreparedResponseData>()

        /**
         * Set up the classes necessary for business-related actions, like
         * recording time, and so on
         */
        fun initializeBusinessCode(
            pmd : PureMemoryDatabase
        ): BusinessCode {
            val cu = CurrentUser(SYSTEM_USER)
            val tep = TimeEntryPersistence(pmd, cu)
            val tru = TimeRecordingUtilities(tep, cu)
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
                DatabaseDiskPersistence.startWithDiskPersistence(dbDirectory)
            }
        }

        fun handleRequest(server: ISocketWrapper, businessCode: BusinessCode) : AnalyzedHttpData {
            lateinit var analyzedHttpData : AnalyzedHttpData
            val responseData: PreparedResponseData = try {
                analyzedHttpData = parseHttpMessage(server, businessCode.au)

                logDebug{ "client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}" }
                if (analyzedHttpData.verb == Verb.CLIENT_CLOSED_CONNECTION) {
                    return analyzedHttpData
                }

                if (analyzedHttpData.verb == Verb.INVALID) {
                    handleBadRequest()
                } else {
                    // if we can just return a static file now, do that...
                    val staticResponse : PreparedResponseData? = staticFileCache[analyzedHttpData.path]
                    if (staticResponse != null) {
                    staticResponse
                    } else {
                        // otherwise review the routing
                        // now that we know who the user is (if they authenticated) we can update the current user
                        val truWithUser = businessCode.tru.changeUser(CurrentUser(analyzedHttpData.user))
                        routeToEndpoint(ServerData(businessCode.au, truWithUser, analyzedHttpData, isAuthenticated(analyzedHttpData.user)))
                    }
                }
            } catch (ex: Exception) {
                // If there ane any complaints whatsoever, we return them here
                handleInternalServerError(ex.message ?: ex.stackTraceToString(), ex.stackTraceToString())
            }

            returnData(server, responseData)
            return analyzedHttpData
        }
    }

}