package coverosR3z.server

import coverosR3z.*
import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthPersistence
import coverosR3z.domainobjects.*
import coverosR3z.logging.LogConfig
import coverosR3z.logging.LogTypes
import coverosR3z.misc.getTime
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.EnterTimeAPI
import org.junit.*
import kotlin.concurrent.thread

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
    private val fakeAuth = FakeAuthenticationUtilities()
    private lateinit var serverThread : Thread

    @Before
    fun init() {
        pmd = PureMemoryDatabase.startMemoryOnly()
        ap = AuthenticationPersistence(pmd)
        serverObject = Server(12345)
        serverThread = thread {
            serverObject.startServer(Server.initializeBusinessCode(pmd))
        }
        if (! serverObject.systemReady) {
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
     * Fastest I've seen is 4088 time entries per second,
     * for five threads and 1000 entries, it took 1.223 seconds
     */
    @Test
    fun testEnterTime_PERFORMANCE() {
        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        val newUser = ap.createUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        ap.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val (time, _) = getTime {
            val threadList = (1..2).map { makeClientThreadRepeatedTimeEntries(100, newProject) }
            threadList.forEach { it.join() }
        }
        println("Time was $time")

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
    @Test
    fun testViewTime_PERFORMANCE() {
        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        val newUser = ap.createUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        ap.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val makeTimeEntriesThreads = (1..2).map { makeClientThreadRepeatedTimeEntries(20, newProject) }
        makeTimeEntriesThreads.forEach { it.join() }

        val (time, _) = getTime {
            val viewTimeEntriesThreads = (1..2).map { makeClientThreadRepeatedRequestsViewTimeEntries(50) }
            viewTimeEntriesThreads.forEach { it.join() }
        }

        println("Time was $time")

        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
        LogConfig.logSettings[LogTypes.AUDIT] = true
    }

    /**
     * How fast to see non-dynamic (but not static) content, authenticated
     *
     * By non-dynamic, I mean: authHomePageHTML
     *
     * fastest was 38,132 responses.  20 threads, 10,000 requests each, in 5.245 seconds
     */
    @Test
    fun testViewStaticContentAuthenticated_PERFORMANCE() {
        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false
        val newUser = ap.createUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        ap.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val (time, _) = getTime {
            val viewTimeEntriesThreads = (1..2).map { makeClientThreadRepeatedRequestsViewHomepage(50) }
            viewTimeEntriesThreads.forEach { it.join() }
        }

        println("Time was $time")

        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
        LogConfig.logSettings[LogTypes.AUDIT] = true
    }

    /**
     * How fast to see static content, non-authenticated
     *
     * Very odd.  This one is slower than [testViewStaticContentAuthenticated_PERFORMANCE]
     * Fastest I saw was 60,168 responses per second, 20 threads doing 10,000 requests each in 3.324 seconds.
     *
     * Is there a bug?
     */
    @Test
    fun testViewStaticContentUnauthenticated_PERFORMANCE() {
        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        LogConfig.logSettings[LogTypes.AUDIT] = false

        // warm up the cache first
        val thread1 = makeClientThreadRepeatedRequestsGetStaticFile(2)
        thread1.join()


        val (time, _) = getTime {
            val viewTimeEntriesThreads = (1..5).map { makeClientThreadRepeatedRequestsGetStaticFile(10) }
            viewTimeEntriesThreads.forEach { it.join() }
        }

        println("Time was $time")

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
    private fun makeClientThreadRepeatedRequestsViewTimeEntries(numRequests : Int): Thread {
        return thread {
            val client =
                Client.make(Verb.GET, NamedPaths.TIMEENTRIES.path, listOf("Connection: keep-alive", "Cookie: sessionId=abc123"), authUtilities = fakeAuth)
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
    private fun makeClientThreadRepeatedRequestsViewHomepage(numRequests : Int): Thread {
        return thread {
            val client =
                Client.make(Verb.GET, NamedPaths.TIMEENTRIES.path, listOf("Connection: keep-alive", "Cookie: sessionId=abc123"), authUtilities = fakeAuth)
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
    private fun makeClientThreadRepeatedRequestsGetStaticFile(numRequests : Int): Thread {
        return thread {
            val client =
                Client.make(Verb.GET, "sample.js", listOf("Connection: keep-alive"), authUtilities = fakeAuth)
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
    private fun makeClientThreadRepeatedTimeEntries(numRequests: Int, project: Project): Thread {
        return thread {

            val client =
                Client.make(
                    Verb.POST,
                    NamedPaths.ENTER_TIME.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=abc123"),
                    authUtilities = fakeAuth
                )
            for (i in 1..numRequests) {
                val data = mapOf(
                    EnterTimeAPI.Elements.DATE_INPUT.elemName to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + i / 20).stringValue,
                    EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "some details go here",
                    EnterTimeAPI.Elements.PROJECT_INPUT.elemName to project.id.value.toString(),
                    EnterTimeAPI.Elements.TIME_INPUT.elemName to "1",
                )
                val clientWithData = client.addPostData(data)
                clientWithData.send()
                val result = clientWithData.read()
                Assert.assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }

}