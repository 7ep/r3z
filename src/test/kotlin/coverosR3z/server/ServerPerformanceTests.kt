package coverosR3z.server

import coverosR3z.*
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.domainobjects.Date
import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.Project
import coverosR3z.logging.LogConfig
import coverosR3z.logging.LogTypes
import coverosR3z.misc.getTime
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.EnterTimeAPI
import org.junit.*
import java.net.Socket
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

    private lateinit var client : SocketWrapper
    private lateinit var serverObject : Server
    private lateinit var pmd : PureMemoryDatabase

    @Before
    fun init() {
        serverObject = Server(12345)
        pmd = PureMemoryDatabase.startMemoryOnly()
        thread { serverObject.startServer(Server.initializeBusinessCode(pmd)) }
        val clientSocket = Socket("localhost", 12345)
        client = SocketWrapper(clientSocket, "client")
    }

    @After
    fun stopServer() {
        Server.halfOpenServerSocket.close()
    }

    /**
     * I used this to see just how fast the server ran.  Able to get
     * 25,000 requests per second on 12/26/2020
     */
    @Ignore
    @Test
    fun testEnterTime_PERFORMANCE() {
        // so we don't see spam
        LogConfig.logSettings[LogTypes.DEBUG] = false
        val newProject = pmd.addNewProject(DEFAULT_PROJECT_NAME)
        val newUser = pmd.addNewUser(DEFAULT_USER.name, Hash.createHash(DEFAULT_PASSWORD, DEFAULT_SALT), DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
        pmd.addNewSession("abc123", newUser, DEFAULT_DATETIME)

        val (time, _) = getTime {
            val threadList = (1..8).map {  makeClientThreadRepeatedTimeEntries(10, newProject) }
            threadList.forEach { it.join() }
        }
        println("Time was $time")
        // turn logging back on for other tests
        LogConfig.logSettings[LogTypes.DEBUG] = true
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
     * Enters time for a user on many days
     */
    private fun makeClientThreadRepeatedTimeEntries(numRequests: Int, project: Project): Thread {
        return thread {

            val client =
                Client.make(
                    Verb.POST,
                    NamedPaths.ENTER_TIME.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=abc123"),
                    authUtilities = FakeAuthenticationUtilities()
                )
            for (i in 1..numRequests) {
                val data = mapOf(
                    EnterTimeAPI.Elements.DATE_INPUT.elemName to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + i / 100).stringValue,
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