package coverosR3z.system.utility

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.system.config.SIZE_OF_DECENT_PASSWORD
import coverosR3z.system.config.utility.SystemOptions
import coverosR3z.system.logging.ILogger
import coverosR3z.system.logging.ILogger.Companion.logImperative
import coverosR3z.system.logging.Logger
import coverosR3z.system.misc.utility.generateRandomString
import coverosR3z.persistence.types.SimpleConcurrentSet
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerObjects
import coverosR3z.server.utility.SSLServer
import coverosR3z.server.utility.Server
import coverosR3z.server.utility.StaticFilesUtilities
import coverosR3z.system.config.persistence.SystemConfigurationPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import java.io.File
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This serves as a central location for the code
 * needed to start the system, called by Main.
 */
class FullSystem private constructor(
    val pmd: PureMemoryDatabase,
    val logger: ILogger,
) {

    lateinit var esForThreadsInServer: ExecutorService
    lateinit var server: Server
    lateinit var sslServer: SSLServer

    private val runningSockets : SimpleConcurrentSet<Socket> = SimpleConcurrentSet()

    fun addRunningSocket(socket : Socket) {
        runningSockets.add(socket)
        logger.logTrace { "added a socket (count: ${runningSockets.size})" }
    }

    fun removeRunningSocket(socket : Socket) {
        runningSockets.remove(socket)
        logger.logTrace{"removed a socket (count: ${runningSockets.size})"}
    }

    /**
     * this adds a hook to the Java runtime, so that if the app is running
     * and a user stops it - by pressing ctrl+c or a unix "kill" command - the
     * server socket will be shutdown and some messages about closing the server
     * will log
     */
    fun addShutdownHook(
    ) {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                shutdown()
            })
    }

    /**
     * Systematically shuts down everything in the system,
     */
    fun shutdown(
    ) {
        logImperative("Received shutdown command")
        logImperative("Looping through all sockets with a close command")
        this.runningSockets.forEach { it.close() }

        logImperative("Shutting down the database")
        pmd.stop()

        logImperative("Shutting down the non-ssl server thread")
        server.halfOpenServerSocket.close()

        logImperative("Waiting for the ssl server thread")
        sslServer.sslHalfOpenServerSocket.close()

        logImperative("Shutting down logging")
        logger.stop()

        logImperative("Goodbye world!")
    }



    companion object {

        /**
         * Kicks off a multitude of components, including the database
         * and the server
         *
         * The parameters are just here to enable testing.  A standard system
         * can be run without any of the default params
         *
         * @param systemOptions mandatory - sets options for the system
         * @param logger the [ILogger] to be used throughout the system
         * @param pmd the [PureMemoryDatabase].  You only need to set this for testing.  Otherwise, ignore it
         */
        fun startSystem(
            // get the user's choices from the command line
            systemOptions: SystemOptions = SystemOptions(),

            // start an executor service which will handle the threads inside the server
            esForThreadsInServer: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory()),

            // the logger has to be one of the first things to start in the system
            logger : ILogger = Logger(esForThreadsInServer),

            // start the database
            pmd : PureMemoryDatabase = makeDatabase(dbDirectory = systemOptions.dbDirectory, logger = logger, executorService = esForThreadsInServer),
        ) : FullSystem {
            // initialize access to the system configuration data persistence,
            val scp = SystemConfigurationPersistence(pmd)

            // set the logging from stored configuration
            scp.getSystemConfig()?.logSettings?.let { logger.logSettings = it }

            // potentially override the logging settings from the command line,
            // but this does not impact the written configuration in the database.
            logger.configureLogging(systemOptions)

            // get the static files like CSS, JS, etc loaded into a cache
            val staticFileCache = mutableMapOf<String, PreparedResponseData>()
            StaticFilesUtilities.loadStaticFilesToCache(staticFileCache)

            // create a package of data that is needed by the servers,
            // such as the static file cache
            val serverObjects = ServerObjects(
                staticFileCache,
                logger,
                systemOptions.port,
                systemOptions.sslPort,
                systemOptions.allowInsecure,
                systemOptions.host,
                scp
            )

            // instantiate a system object, we'll need this when starting the servers
            val fullSystem = FullSystem(pmd, logger)

            // start the regular server
            val server = Server(systemOptions.port, esForThreadsInServer, serverObjects, fullSystem)
            esForThreadsInServer.execute(server.createServerThread())

            // start the ssl server
            val sslServer = SSLServer(systemOptions.sslPort, esForThreadsInServer, serverObjects, fullSystem)
            esForThreadsInServer.execute(sslServer.createSecureServerThread())

            // Add an Administrator employee and role if the database is empty
            if (pmd.isEmpty()) {
                initializeDataForEmptyDatabase(pmd, logger)
            }

            fullSystem.esForThreadsInServer = esForThreadsInServer
            fullSystem.server = server
            fullSystem.sslServer = sslServer

            fullSystem.addShutdownHook()

            return fullSystem
        }

        /**
         * There needs to be certain data added if we find out we're
         * dealing with an entirely new, entirely empty database.
         *
         * For example, we need to add an Administrator employee and role.
         */
        private fun initializeDataForEmptyDatabase(
            pmd: PureMemoryDatabase,
            logger: ILogger,
        ) {
            val bc = initializeBusinessCode(pmd, logger)
            val mrAdmin = bc.tru.createEmployee(EmployeeName("Administrator"))
            logImperative("Created an initial employee")
            val password = generateRandomString(SIZE_OF_DECENT_PASSWORD)
            val username = "administrator"
            val (_, user) = bc.au.registerWithEmployee(
                UserName(username),
                Password(password),
                mrAdmin
            )
            bc.au.addRoleToUser(user, Role.ADMIN)
            logImperative("Created an initial user, \"$username\", with password: $password")
            val accountCredentialsFile = "admin_acct.txt"
            File(accountCredentialsFile).writeText("username: $username\npassword: $password")
            logImperative("Wrote account credentials to \"$accountCredentialsFile\"")
        }


        /**
         * Set up the classes necessary for business-related actions, like
         * recording time, and so on
         */
        fun initializeBusinessCode(
            pmd : PureMemoryDatabase,
            logger: ILogger,
            cu: CurrentUser = CurrentUser(SYSTEM_USER)
        ): BusinessCode {
            val tep = TimeEntryPersistence(pmd, cu, logger)
            val tru = TimeRecordingUtilities(tep, cu, logger)

            val ap = AuthenticationPersistence(pmd, logger)
            val au = AuthenticationUtilities(ap, logger)

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
        private fun makeDatabase(
            pmd: PureMemoryDatabase? = null,
            dbDirectory: String? = null,
            logger: ILogger,
            executorService: ExecutorService
        ): PureMemoryDatabase {
            logImperative("database directory is $dbDirectory")
            return pmd ?: if (dbDirectory == null) {
                PureMemoryDatabase.createEmptyDatabase()
            } else {
                DatabaseDiskPersistence(dbDirectory, logger, executorService).startWithDiskPersistence()
            }
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