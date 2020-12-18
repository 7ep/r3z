package coverosR3z.server

import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.domainobjects.DateTime
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.exceptions.ServerOptionsException
import coverosR3z.logging.*
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.ITimeRecordingUtilities
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.Executors

/**
 * This is the top-level entry into the web server
 * @param port the port the server will run on
 * @param dbDirectory the directory to store data.  If no directory is provided, the database
 *                    will run entirely in memory with no disk persistence taking place.
 */
class Server(val port: Int, private val dbDirectory: String? = null) {

    fun startServer(authUtils: IAuthenticationUtilities? = null) {
        halfOpenServerSocket = ServerSocket(port)

        val cu = CurrentUser(SYSTEM_USER)
        val pmd = if (dbDirectory == null) {PureMemoryDatabase.startMemoryOnly()} else {PureMemoryDatabase.startWithDiskPersistence(dbDirectory)}
        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), cu)
        val au = authUtils ?: AuthenticationUtilities(AuthenticationPersistence(pmd))
        logStart("System is ready.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")

        val cachedThreadPool = Executors.newCachedThreadPool()
        try {
            while (true) {
                logTrace("waiting for socket connection")

                val server = SocketWrapper(halfOpenServerSocket.accept(), "server")
                val thread = Thread {
                    logTrace("client from ${server.socket.inetAddress?.hostAddress} has connected")
                    do {
                        val requestData = handleRequest(server, au, tru)
                        val shouldKeepAlive = requestData.headers.any { it.toLowerCase().contains("connection: keep-alive") }
                        if (shouldKeepAlive) {
                            logTrace("This is a keep-alive connection")
                        }
                    } while (shouldKeepAlive)

                    logTrace("closing server socket")
                    server.close()
                }
                cachedThreadPool.submit(thread)
            }
        } catch (ex : SocketException) {
            if (ex.message == "Interrupted function call: accept failed") {
                logWarn("Server was shutdown while waiting on accept")
            }
        }
    }

    companion object {

        lateinit var halfOpenServerSocket : ServerSocket

        /**
         * Given the command-line arguments, returns the first value
         * as an integer for use as a port number, or defaults to 12345
         */
        fun extractOptions(args: Array<String>) : ServerOptions {
            val fullInput = args.joinToString(" ")
//            var portOptionIndex : Int = args.indexOfFirst { it.startsWith("-p") }
            return if (args.isEmpty() || args[0].isBlank()) {
//            return if (args.isEmpty() || args[0].isBlank() || portOptionIndex==-1) {
                ServerOptions()
            } else {
                var port : Int? = null
                var db : String? = null
                var otherArgs: MutableList<String> = mutableListOf()
                for (i in args.indices) {

                    if (args[i].startsWith("--")) {
                        if (args[i].length > 2) otherArgs.add(args[i].substring(2))

                    } else if (args[i].startsWith("-p")) {
                        if (args[i].length > 2) port = args[i].substring(2).toIntOrNull()
                        else if (i+1 < args.size-1) {
                            port = args[i + 1].toIntOrNull()
                        }
                    } else if (args[i].startsWith("-d")) {
                        if (args[i].length > 2) db = args[i].substring(2)
                        else if (i+1 < args.size-1){
                            db = args[i + 1]
                        }
                    }
                }
                if (db == null && otherArgs.isEmpty() && port == null) {
                    throw ServerOptionsException("The directory option was provided without a directory value")
                } else if (port != null && port !in 1..65535) {
                    throw ServerOptionsException("port number was out of range.  Range is 1-65535.  Your input was: $fullInput")
                } else if (port != null && db != null){
                    ServerOptions(port, db)
                } else if (port != null && db == null) {
                    if (otherArgs.contains("no-disk-persistence")){
                        ServerOptions(port, dbDirectory=null)
                    } else {
                        ServerOptions(port)
                    }
                } else if (otherArgs.contains("no-disk-persistence") && db == null) {
                    ServerOptions(dbDirectory=null)
                } else if (otherArgs.contains("no-disk-persistence") && db != null) {
                    throw ServerOptionsException("You cannot combine options to set the database directory with disallowing disk persistence")
                }
                else {
                    ServerOptions(dbDirectory=db)
                }

            }
        }

        fun handleRequest(server: ISocketWrapper, au: IAuthenticationUtilities, tru: ITimeRecordingUtilities) : AnalyzedHttpData {
            lateinit var analyzedHttpData : AnalyzedHttpData
            val responseData: PreparedResponseData = try {
                analyzedHttpData = parseHttpMessage(server, au)
                if (analyzedHttpData.verb == Verb.CLIENT_CLOSED_CONNECTION) {
                    return analyzedHttpData
                }
                // now that we know who the user is (if they authenticated) we can update the current user
                val cu = CurrentUser(analyzedHttpData.user)
                val truWithUser = tru.changeUser(cu)
                logDebug("client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}", cu)
                handleRequestAndRespond(ServerData(au, truWithUser, analyzedHttpData))
            } catch (ex: Exception) {
                handleInternalServerError(ex.message, ex.stackTraceToString())
            }

            returnData(server, responseData)
            return analyzedHttpData
        }
    }

}