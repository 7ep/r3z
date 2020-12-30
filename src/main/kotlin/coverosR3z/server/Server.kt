package coverosR3z.server

import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.DateTime
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.exceptions.ServerOptionsException
import coverosR3z.logging.*
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * This is the top-level entry into the web server
 * @param port the port the server will run on
 */
class Server(val port: Int) {


    lateinit var halfOpenServerSocket : ServerSocket
    lateinit var cachedThreadPool: ExecutorService
    var systemReady = false

    /**
     * This requires a [BusinessCode] object, see [initializeBusinessCode]
     * for the typical way to create this.
     */
    fun startServer(businessObjects : BusinessCode) {
        halfOpenServerSocket = ServerSocket(port)

        try {
            cachedThreadPool = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
            systemReady = true
            logImperative("System is ready.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")
            while (true) {
                logTrace("waiting for socket connection")
                val server = SocketWrapper(halfOpenServerSocket.accept(), "server")
                cachedThreadPool.submit(Thread {processConnectedClient(server, businessObjects)})
            }
        } catch (ex : SocketException) {
            if (ex.message == "Interrupted function call: accept failed") {
                logImperative("Server was shutdown while waiting on accept")
                systemReady = false
            }
        }
    }

    private fun processConnectedClient(
        server: SocketWrapper,
        businessCode: BusinessCode
    ) {
        logTrace("client from ${server.socket.inetAddress?.hostAddress} has connected")
        do {
            val requestData = handleRequest(server, businessCode)
            val shouldKeepAlive = requestData.headers.any { it.toLowerCase().contains("connection: keep-alive") }
            if (shouldKeepAlive) {
                logTrace("This is a keep-alive connection")
            }
        } while (shouldKeepAlive)

        logTrace("closing server socket")
        server.close()
    }

    /**
     * this adds a hook to the Java runtime, so that if the app is running
     * and a user stops it - by pressing ctrl+c or a unix "kill" command - the
     * server socket will be shutdown and some messages about closing the server
     * will log
     */
    fun addShutdownHook(pmd: PureMemoryDatabase) : Server {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                serverShutdown(pmd)
            })
        return this
    }

    fun serverShutdown(pmd: PureMemoryDatabase) {
        logImperative("Received shutdown command")
        logImperative("Shutting down main server thread")
        cachedThreadPool.shutdown()
        cachedThreadPool.awaitTermination(10, TimeUnit.SECONDS)
        halfOpenServerSocket.close()

        logImperative("Shutting down logging")
        loggerPrinter.stop()

        logImperative("Shutting down the database")
        pmd.stop()

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

        /**
         * Given the command-line arguments, returns the first value
         * as an integer for use as a port number, or defaults to 12345
         */
        fun extractOptions(args: Array<String>) : ServerOptions {

            try {
                return processArgs(args)
            }
            catch (ex: Throwable) {
                throw ServerOptionsException(ex.message + "\n" +fullHelpMessage)
            }
        }

        private fun processArgs(args: Array<String>): ServerOptions {
            /**
             * Holds the data for a particular option provided on the command line.  See [ServerOptions.make]
             * for accepting these values and performing validation.
             *
             * The only validation we do here is disallowing duplicate values, like "-p 10 -p 20,"
             * and also that the value for an option doesn't start with a dash, like "-d -thisiswrong",
             * or that a key requiring a value does indeed have one, unlike: "-d "
             */
            data class Option(var setByUser : Boolean, var value : String, val isFlag : Boolean = false)

            /**
             * An option group is a group of ways to access an option, say maybe you
             * have a short version ("-p") and a long version("--port").
             * Just have multiple OptionGroup that refer to the same Option
             */
            data class OptionGroup(val textValue : String, val o : Option)

            val directoryOption = Option(false, "")
            val portOption = Option(false, "")
            val diskPersistenceOption = Option(false, "", isFlag = true)

            val possibleOptions = listOf(
                OptionGroup("-d", directoryOption),
                OptionGroup("--dbdirectory", directoryOption),
                OptionGroup("-p", portOption),
                OptionGroup("--port", portOption),
                OptionGroup("--no-disk-persistence", diskPersistenceOption))

            val fullInput = args.joinToString(" ")
            return if (args.isEmpty() || args[0].isBlank()) {
                ServerOptions()
            } else {
                var currentIndex = 0

                loop@ while (currentIndex < args.size) {
                    if (args[currentIndex] == "-h" || args[currentIndex] == "-?") throw ServerOptionsException("")

                    for (option in possibleOptions) {
                        if (args[currentIndex].startsWith(option.textValue)) {
                            if (option.o.setByUser) throw ServerOptionsException("Duplicate options were provided. This is not allowed, go to jail. your input: $fullInput")
                            val (value, increment) = if (option.o.isFlag) Pair("true", 1) else getValueForKey(option.textValue, args, currentIndex, fullInput)
                            currentIndex += increment
                            option.o.setByUser = true
                            option.o.value = value
                            continue@loop
                        }
                    }
                    throw ServerOptionsException("argument not recognized: ${args[currentIndex]}")
                }

                ServerOptions.make(portOption.value, directoryOption.value, diskPersistenceOption.value)
            }
        }

        /**
         * Gets the value for a flag.  Returns a pair with the value as the first entry and the increment for the current index as the second entry
         */
        private fun getValueForKey(keyString : String, args: Array<String>, currentIndex: Int, fullInput: String) : Pair<String, Int> {
            val concatValue = args[currentIndex].substring(keyString.length)
            return if (concatValue.isNotEmpty()) {
                check(concatValue.isNotBlank())
                if (concatValue.startsWith("-")) throw ServerOptionsException("The $keyString option was provided without a value: $fullInput")
                Pair(concatValue, 1)
            } else {
                val value = try {args[currentIndex + 1]} catch (ex : IndexOutOfBoundsException) {throw ServerOptionsException("The $keyString option was provided without a value: $fullInput")}
                if (value.startsWith("-")) throw ServerOptionsException("The $keyString option was provided without a value: $fullInput")
                Pair(value, 2)
            }
        }

        val fullHelpMessage = """
Here is some help for running this application.
        
You can provide options when running this, to change its configuration.

Sample: 
    The following runs the application with the
    port set to 54321 and the database directory
    set to "db/" (make sure to end the directory
    with a forward slash):
    
    java -jar r3z.jar -p 54321 -d db/
    
The options available are:

-h                     prints this help message
-p PORT_NUMBER         set the port number for the server
-d DIRECTORY           the directory to store data
--no-disk-persistence  do not write data to the disk.  Note
                       that this is primarily (exclusively?) for testing
    """.trimIndent()


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
                logDebug("client requested ${analyzedHttpData.verb} /${analyzedHttpData.path}", cu)
                handleRequestAndRespond(ServerData(businessCode.au, truWithUser, analyzedHttpData))
            } catch (ex: Exception) {
                handleInternalServerError(ex.message, ex.stackTraceToString())
            }

            returnData(server, responseData)
            return analyzedHttpData
        }
    }

}