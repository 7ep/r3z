package coverosR3z.server

import coverosR3z.FullSystem
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.config.utility.SystemOptions
import coverosR3z.misc.*
import coverosR3z.misc.utility.getTime
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.StatusCode
import coverosR3z.server.types.Verb
import coverosR3z.techempower.utility.ITechempowerUtilities
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.types.Project
import org.junit.*
import org.junit.experimental.categories.Category
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

    private lateinit var fs : FullSystem

    private fun startServer(port : Int) {
        val pmd = PureMemoryDatabase(worlds = ITechempowerUtilities.generateWorlds())
        fs = FullSystem.startSystem(SystemOptions(port = port, sslPort = port + 443, allLoggingOff = true), pmd = pmd)
    }

    @After
    fun stopServer() {
        fs.shutdown()
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
    @Category(PerformanceTestCategory::class)
    @Test
    fun testEnterTimeReal_PERFORMANCE() {
        val numberThreads = 5
        val numberRequests = 200

        val (newProject, sessionId, es: ExecutorService) = setupForTest()

        val (time, _) = getTime {
            val realThreads = (1..numberThreads).map { es.submit(makeClientThreadRepeatedTimeEntries(numberRequests, newProject, sessionId)) }
            realThreads.forEach { it.get() }
            es.shutdown()
            es.awaitTermination(10, TimeUnit.SECONDS)
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testEnterTimeReal_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")

    }

    /**
     * How fast to see data, the user's time entries.
     *
     * Fastest I've seen is 12,801 responses per second
     * with 200 threads and 1000 requests each,
     * that's 200,000 requests in 15.623 seconds,
     */
    @IntegrationTest(usesPort = true)
    @Category(PerformanceTestCategory::class)
    @Test
    fun testViewTime_PERFORMANCE() {
        val numberThreads = 20
        val numberRequests = 100

        val (newProject, sessionId, es: ExecutorService) = setupForTest()

        val makeTimeEntriesThreads = (1..2).map { es.submit(makeClientThreadRepeatedTimeEntries(20, newProject, sessionId)) }
        makeTimeEntriesThreads.forEach { it.get() }

        val (time, _) = getTime {
            val realThreads = (1..numberThreads).map { es.submit(makeClientThreadRepeatedRequestsViewTimeEntries(numberRequests, sessionId)) }
            realThreads.forEach { it.get() }
            es.shutdown()
            es.awaitTermination(10, TimeUnit.SECONDS)
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testViewTime_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")
    }

    private fun setupForTest(): Triple<Project, String, ExecutorService> {
        val port = port.getAndIncrement()
        startServer(port)
        val tru = fs.businessCode.tru.changeUser(CurrentUser(DEFAULT_ADMIN_USER))
        val newProject = tru.createProject(DEFAULT_PROJECT_NAME)
        val employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val (_, user) = fs.businessCode.au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, employee.id)
        val sessionId = fs.businessCode.au.createNewSession(user)

        val es: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        return Triple(newProject, sessionId, es)
    }

    /**
     * How fast to see non-dynamic (but not static) content, authenticated
     *
     * By non-dynamic, I mean: authHomePageHTML
     *
     * fastest was 41,946 responses.  20 threads, 10,000 requests each, in 4.768 seconds
     */
    @IntegrationTest(usesPort = true)
    @Category(PerformanceTestCategory::class)
    @Test
    fun testViewStaticContentAuthenticated_PERFORMANCE() {
        val numberThreads = 20
        val numberRequests = 500

        val port = port.getAndIncrement()
        startServer(port)

        val employee = fs.businessCode.tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val (_, user) = fs.businessCode.au.register(DEFAULT_USER.name,DEFAULT_PASSWORD, employee.id)
        val sessionId = fs.businessCode.au.createNewSession(user)

        val es: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val (time, _) = getTime {
            val realThreads = (1..numberThreads).map { es.submit(makeClientThreadRepeatedRequestsViewHomepage(numberRequests, sessionId)) }
            realThreads.forEach { it.get() }
            es.shutdown()
            es.awaitTermination(10, TimeUnit.SECONDS)
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testViewStaticContentAuthenticated_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")
    }

    /**
     * How fast to see static content, non-authenticated
     *
     * Fastest I saw was 73,746 responses per second, 20 threads doing 10,000 requests each in 2.712 seconds.
     *
     */
    @IntegrationTest(usesPort = true)
    @Category(PerformanceTestCategory::class)
    @Test
    fun testViewStaticContentUnauthenticated_PERFORMANCE() {
        val numberThreads = 10
        val numberRequests = 500

        val port = port.getAndIncrement()
        startServer(port)

        val es: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val (time, _) = getTime {
            val realThreads = (1..numberThreads).map { es.submit(makeClientThreadRepeatedRequestsGetStaticFile(numberRequests)) }
            realThreads.forEach { it.get() }
            es.shutdown()
            es.awaitTermination(10, TimeUnit.SECONDS)
        }

        println("Time was $time")
        File("${granularPerfArchiveDirectory}testViewStaticContentUnauthenticated_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberThreads: $numberThreads\tnumberRequests: $numberRequests\ttime: $time\n")
    }


    @IntegrationTest(usesPort = true)
    @Category(PerformanceTestCategory::class)
    @Test
    fun testTechempowerAPI_PERFORMANCE() {
        val numberThreads = 5
        val numberRequests = 2

        val port = port.getAndIncrement()
        startServer(port)

        val worlds = ITechempowerUtilities.generateWorlds()
        fs.businessCode.tu.addRows(worlds)
        val es: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        val (time, _) = getTime {
            val realThreads = (1..numberThreads).map { es.submit(makeClientForTechempower(numberRequests)) }
            realThreads.forEach { it.get() }
            es.shutdown()
            es.awaitTermination(10, TimeUnit.SECONDS)
        }

        println("Time was $time")
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
    private fun makeClientThreadRepeatedRequestsViewTimeEntries(numRequests : Int, sessionId: String): Thread {
        return Thread {
            val client =
                Client.make(
                    Verb.GET,
                    ViewTimeAPI.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=$sessionId"),
                    authUtilities = fs.businessCode.au,
                    port = fs.server.port)
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
    private fun makeClientThreadRepeatedRequestsViewHomepage(numRequests : Int, sessionId: String): Thread {
        return Thread {
            val client =
                Client.make(
                    Verb.GET,
                    ViewTimeAPI.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=$sessionId"),
                    authUtilities = fs.businessCode.au,
                    port = fs.server.port)
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
        return Thread {
            val client =
                Client.make(
                    Verb.GET,
                    "sample.js",
                    listOf("Connection: keep-alive"),
                    authUtilities = fs.businessCode.au,
                    port = fs.server.port)
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
    private fun makeClientThreadRepeatedTimeEntries(numRequests: Int, project: Project, sessionId: String): Thread {
        return Thread {

            val client =
                Client.make(
                    Verb.POST,
                    EnterTimeAPI.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=$sessionId"),
                    authUtilities = fs.businessCode.au,
                    port = fs.server.port
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



    /**
     * Simply GETs the general.css file
     */
    private fun makeClientForTechempower(numRequests : Int): Thread {
        return Thread {
            val client =
                Client.make(
                    Verb.GET,
                    "updates?queries=20",
                    listOf("Connection: keep-alive"),
                    authUtilities = fs.businessCode.au,
                    port = fs.server.port)
            for (i in 1..numRequests) {
                client.send()
                val result = client.read()
                Assert.assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }


    companion object {
        val port = AtomicInteger(3000)
    }

}