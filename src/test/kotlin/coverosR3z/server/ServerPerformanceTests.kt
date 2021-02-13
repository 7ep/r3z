package coverosR3z.server

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.persistence.IAuthPersistence
import coverosR3z.authentication.types.Hash
import coverosR3z.logging.LogConfig
import coverosR3z.logging.LogTypes
import coverosR3z.misc.*
import coverosR3z.misc.utility.getTime
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.utility.Server
import coverosR3z.server.types.StatusCode
import coverosR3z.server.types.Verb
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.Project
import org.junit.*
import java.io.File
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * This is a heavyweight full-integration testing harness for
 * examining the performance of the system as it really runs.
 *
 * One note is that the database runs here in memory-only mode
 *
 * No fakes here.  If you want that, to see just how the server
 * part works, see [ServerTests]
 */
class ServerPerformanceTests {

    private lateinit var serverObject : Server
    private lateinit var pmd : PureMemoryDatabase
    private lateinit var ap : IAuthPersistence
    private lateinit var tep : ITimeEntryPersistence
    private val fakeAuth = FakeAuthenticationUtilities()
    private lateinit var serverThread : Thread

    @Before
    fun init() {
        pmd = PureMemoryDatabase.startMemoryOnly()
        ap = AuthenticationPersistence(pmd)
        tep = TimeEntryPersistence(pmd)
    }

    private fun startServer(port : Int) {
        serverObject = Server(port)
        serverThread = serverObject.createServerThread(Server.initializeBusinessCode(pmd))
        val executor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
        executor.submit(serverThread)
        if (!serverObject.systemReady) {
            Thread.sleep(50)
        }
    }

    @After
    fun stopServer() {
        // shutdown the server socket so the sockets
        // don't go into TIME_WAIT
        serverObject.halfOpenServerSocket.close()
    }

    /**
     * How fast to enter data, the user's time entries
     *
     * Fastest I've seen is 11,876 time entries per second,
     * for five threads and 1000 requests (5000 time entries), it took .421 seconds
     *
     * See EnterTimeAPITests.testEnterTimeAPI_PERFORMANCE for a lower-level version of this
     *
     */
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testEnterTimeReal_PERFORMANCE() {
        val numberThreads = 5
        val numberRequests = 200

        val port = port.getAndIncrement()
        startServer(port)

        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newUser = ap.createUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        ap.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val (time, _) = getTime {
            val threadList = (1..numberThreads).map { cachedThreadPool.submit(makeClientThreadRepeatedTimeEntries(numberRequests, newProject, port)) }
            threadList.forEach { it.get() }
        }
        println("Time was $time")
        File("${granularPerfArchiveDirectory}testEnterTimeReal_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")

        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
        LogConfig.logSettings[LogTypes.AUDIT] = true
    }

    /**
     * How fast to see data, the user's time entries.
     *
     * Fastest I've seen is 12,801 responses per second
     * with 200 threads and 1000 requests each,
     * that's 200,000 requests in 15.623 seconds,
     */
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testViewTime_PERFORMANCE() {
        val numberThreads = 20
        val numberRequests = 100

        val port = port.getAndIncrement()
        startServer(port)

        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newUser = ap.createUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        ap.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val makeTimeEntriesThreads = (1..2).map { cachedThreadPool.submit(makeClientThreadRepeatedTimeEntries(20, newProject, port)) }
        makeTimeEntriesThreads.forEach { it.get() }

        val (time, _) = getTime {
            val viewTimeEntriesThreads = (1..numberThreads).map { cachedThreadPool.submit(makeClientThreadRepeatedRequestsViewTimeEntries(numberRequests, port)) }
            viewTimeEntriesThreads.forEach { it.get() }
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testViewTime_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")

        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
        LogConfig.logSettings[LogTypes.AUDIT] = true
    }

    /**
     * How fast to see non-dynamic (but not static) content, authenticated
     *
     * By non-dynamic, I mean: authHomePageHTML
     *
     * fastest was 41,946 responses.  20 threads, 10,000 requests each, in 4.768 seconds
     */
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testViewStaticContentAuthenticated_PERFORMANCE() {
        val numberThreads = 20
        val numberRequests = 500

        val port = port.getAndIncrement()
        startServer(port)

        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false
        val newUser = ap.createUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        ap.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val (time, _) = getTime {
            val viewTimeEntriesThreads = (1..numberThreads).map { cachedThreadPool.submit(makeClientThreadRepeatedRequestsViewHomepage(numberRequests, port)) }
            viewTimeEntriesThreads.forEach { it.get() }
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testViewStaticContentAuthenticated_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")

        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
        LogConfig.logSettings[LogTypes.AUDIT] = true
    }

    /**
     * How fast to see static content, non-authenticated
     *
     * Fastest I saw was 73,746 responses per second, 20 threads doing 10,000 requests each in 2.712 seconds.
     *
     */
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testViewStaticContentUnauthenticated_PERFORMANCE() {
        val numberThreads = 10
        val numberRequests = 500

        val port = port.getAndIncrement()
        startServer(port)

        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false

        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val (time, _) = getTime {
            val viewTimeEntriesThreads = (1..numberThreads).map { cachedThreadPool.submit(makeClientThreadRepeatedRequestsGetStaticFile(numberRequests, port)) }
            viewTimeEntriesThreads.forEach { it.get() }
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testViewStaticContentUnauthenticated_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")

        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
        LogConfig.logSettings[LogTypes.AUDIT] = true
    }


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */


    /**
     * Simply GETs a user's time entries page
     */
    private fun makeClientThreadRepeatedRequestsViewTimeEntries(numRequests : Int, port : Int): Thread {
        return Thread {
            val client =
                Client.make(Verb.GET, ViewTimeAPI.path, listOf("Connection: keep-alive", "Cookie: sessionId=abc123"), authUtilities = fakeAuth, port = port)
            for (i in 1..numRequests) {
                client.send()
                val result = client.read()
                Assert.assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }

    /**
     * Simply GETs the homepage
     */
    private fun makeClientThreadRepeatedRequestsViewHomepage(numRequests : Int, port : Int): Thread {
        return Thread {
            val client =
                Client.make(Verb.GET, ViewTimeAPI.path, listOf("Connection: keep-alive", "Cookie: sessionId=abc123"), authUtilities = fakeAuth, port = port)
            for (i in 1..numRequests) {
                client.send()
                val result = client.read()
                Assert.assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }


    /**
     * Simply GETs the general.css file
     */
    private fun makeClientThreadRepeatedRequestsGetStaticFile(numRequests : Int, port : Int): Thread {
        return Thread {
            val client =
                Client.make(Verb.GET, "sample.js", listOf("Connection: keep-alive"), authUtilities = fakeAuth, port = port)
            for (i in 1..numRequests) {
                client.send()
                val result = client.read()
                Assert.assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }

    /**
     * Enters time for a user on many days
     * @param numRequests The number of requests this client will send to the server.
     */
    private fun makeClientThreadRepeatedTimeEntries(numRequests: Int, project: Project, port : Int): Thread {
        return Thread {

            val client =
                Client.make(
                    Verb.POST,
                    EnterTimeAPI.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=abc123"),
                    authUtilities = fakeAuth,
                    port = port
                )
            for (i in 1..numRequests) {
                val data = PostBodyData(mapOf(
                    EnterTimeAPI.Elements.DATE_INPUT.getElemName() to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + i).stringValue,
                    EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "some details go here",
                    EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to project.id.value.toString(),
                    EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                ))
                val clientWithData = client.addPostData(data)
                clientWithData.send()
                val result = clientWithData.read()
                Assert.assertEquals("result was ${result.data.rawData}", StatusCode.SEE_OTHER, result.statusCode)
            }
        }
    }
    
    companion object {
        val port = AtomicInteger(3000)
    }

}