package coverosR3z.server

import coverosR3z.A_RANDOM_DAY_IN_JUNE_2020
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.misc.utility.SystemOptions
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.misc.types.Date
import coverosR3z.authentication.types.NO_USER
import coverosR3z.misc.exceptions.ServerOptionsException
import coverosR3z.logging.LogConfig.logSettings
import coverosR3z.logging.LogTypes
import coverosR3z.misc.utility.encode
import coverosR3z.misc.utility.getTime
import coverosR3z.misc.utility.SystemOptions.Companion.extractOptions
import coverosR3z.misc.utility.SystemOptions.Companion.fullHelpMessage
import coverosR3z.misc.utility.FileReader.Companion.read
import coverosR3z.misc.utility.toStr
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.StatusCode
import coverosR3z.server.types.Verb
import coverosR3z.server.utility.*
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Bear in mind this set of tests is to focus on the server functionality,
 * and *not* the underlying business code / database code.  That's why it
 * is fine to use fakes for the business code.  If you want to see
 * the server running with everything real, see [ServerPerformanceTests]
 */
class ServerTests {

    private lateinit var client : SocketWrapper

    companion object {
        private lateinit var serverObject : Server
        private val au = FakeAuthenticationUtilities()
        private val tru = FakeTimeRecordingUtilities()

        @JvmStatic
        @BeforeClass
        fun initServer() {
            serverObject = Server(12345)
            serverObject.startServer(BusinessCode(tru, au))
        }

        @JvmStatic
        @AfterClass
        fun stopServer() {
            serverObject.halfOpenServerSocket.close()
        }
    }

    @Before
    fun init() {
        val clientSocket = Socket("localhost", 12345)
        client = SocketWrapper(clientSocket, "client")
    }

    /**
     * If we provide no options, it defaults to 12345 and a database directory of "db"
     */
    @Test
    fun testShouldParsePortFromCLI_nothingProvided() {
        val serverOptions = extractOptions(arrayOf())
        assertEquals(SystemOptions(), serverOptions)
    }

    @Test
    fun testShouldParseOptions_Port() {
        val serverOptions = extractOptions(arrayOf("-p","54321"))
        assertEquals(SystemOptions(54321), serverOptions)
    }

    @Test
    fun testShouldParseOptions_PortAnotherValid() {
        val serverOptions = extractOptions(arrayOf("-p","11111"))
        assertEquals(SystemOptions(11111), serverOptions)
    }

    @Test
    fun testShouldParseOptions_weirdDatabaseDirectory() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-d-p1024"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -d option was provided without a value: -d-p1024"))
    }

    @Test
    fun testShouldParseOptions_NoDiskPersistenceOption() {
        val serverOptions = extractOptions(arrayOf("--no-disk-persistence"))
        assertEquals(SystemOptions(12345, null), serverOptions)
    }

    @Test
    fun testShouldParseOptions_PortNoSpace() {
        val serverOptions = extractOptions(arrayOf("-p54321"))
        assertEquals(SystemOptions(54321), serverOptions)
    }

    /**
     * There is no -a option currently
     */
    @Test
    fun testShouldParseOptions_UnrecognizedOptions() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-p", "54321", "-d", "2321", "-a",  "213123"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("argument not recognized: -a"))
    }

    /**
     * The port provided must be an integer between 0 and 65535
     * It will probably complain though if you run this below 1024
     * as non-root (below 1024, you need admin access on the machine, typically)
     */
    @Test
    fun testShouldParseOptions_badPort_nonInteger() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-pabc123"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Must be able to parse abc123 as integer"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_negativeInteger() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-p", "-1"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -p option was provided without a value: -p -1"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_zero() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-p", "0"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: 0"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_above65535() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-p", "65536"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: 65536"))
    }

    /**
     * If we provide no value to the port, complain
     */
    @Test
    fun testShouldParseOptions_badPort_empty() {
        val ex = assertThrows(ServerOptionsException::class.java) {
            extractOptions(arrayOf("-p", "-d", "db"))
        }
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -p option was provided without a value: -p -d db"))
    }

    @Test
    fun testShouldParseOptions_DatabaseDirectory() {
        val serverOptions = extractOptions(arrayOf("-d", "build/db"))
        assertEquals(SystemOptions(dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_BadPort_TooMany() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-p", "22", "-p", "33"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Duplicate options were provided."))
    }

    @Test
    fun testShouldParseOptions_DatabaseDirectoryNoSpace() {
        val serverOptions = extractOptions(arrayOf("-dbuild/db"))
        assertEquals(SystemOptions(dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_badDatabaseDirectory_Empty() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-d"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -d option was provided without a value: -d"))
    }

    @Test
    fun testShouldParseOptions_badDatabaseDirectory_EmptyAlternate() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-d",  "-p1024"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -d option was provided without a value: -d -p1024"))
    }

    /**
     * This is valid but not great
     *
     * This is likely a typo by the user, they probably meant to set
     * multiple parameters, but on the other hand the database
     * option needs a value after, so if our user typed it in
     * like: r3z -d-p1024, yeah, they'll get a directory of -p1024
     * which looks nuts, but we'll allow it.
     *
     * By the way, if the user *does* put in something that the
     * operating system won't allow, they will get a complaint,
     * an exception that stems from the File.write command
     */


    @Test
    fun testShouldParseOptions_multipleOptionsInvalid() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("--no-disk-persistence", "-dbuild/db"))}
        val expected = "If you're setting the noDiskPersistence option and also a database directory, you're very foolish"
        assertTrue("Message needs to match expected; yours was:\n${ex.message}", ex.message!!.contains(expected))
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation1() {
        val serverOptions = extractOptions(arrayOf("-p54321", "-dbuild/db"))
        assertEquals(SystemOptions(54321, "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation2() {
        val serverOptions = extractOptions(arrayOf("-dbuild/db", "-p54321"))
        assertEquals(SystemOptions(54321, "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation3() {
        val serverOptions = extractOptions(arrayOf("-dbuild/db", "-p", "54321"))
        assertEquals(SystemOptions(54321, "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation4() {
        val serverOptions = extractOptions(arrayOf("-d", "build/db", "-p", "54321"))
        assertEquals(SystemOptions(54321, "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_setAllLoggingOff() {
        val serverOptions = extractOptions(arrayOf("-d", "build/db", "-p", "54321", "--no-logging"))
        assertEquals(SystemOptions(54321, "build/db/", allLoggingOff = true), serverOptions)
    }

    /**
     * If the user asks for help with -h or -?, provide
     * an explanation of the app options
     */
    @Test
    fun testShouldHelpUser() {
        val ex = assertThrows(ServerOptionsException::class.java) {extractOptions(arrayOf("-h"))}
        assertEquals(fullHelpMessage, ex.message!!.trimIndent())
    }

    /**
     * If we try something and are unauthenticated,
     * receive a 401 error page
     */
    @Test
    fun testShouldReturnUnauthenticatedAs401Page() {
        client.write("POST /entertime HTTP/1.1$CRLF")
        val body = "test=test"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 401 UNAUTHORIZED", statusline)
    }

    /**
     * If we ask for the homepage, we'll get a 200 OK
     */
    @Test
    fun testShouldGet200Response() {
        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 200 OK", statusline)
    }

    /**
     * If the client asks for a file, give it
     */
    @Test
    fun testShouldGetFileResponse() {
        client.write("GET /sample.html HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.html")!!), result.rawData)
    }

    /**
     * If the client asks for a file, give it
     * CSS edition
     */
    @Test
    fun testShouldGetFileResponse_CSS() {
        client.write("GET /sample.css HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.css")!!), result.rawData)
    }

    /**
     * If the client asks for a file, give it
     * JS edition
     */
    @Test
    fun testShouldGetFileResponse_JS() {
        client.write("GET /sample.js HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.js")!!), result.rawData)
    }

    /**
     * Action for an invalid request
     */
    @Test
    fun testShouldParseMultipleClientRequestTypes_BadRequest() {
        client.write("INVALID /test.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.BAD_REQUEST, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        client.write("GET /doesnotexist.html HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server does have, but it's not a suffix we recognize?
     * See ServerUtilties.handleUnknownFiles
     */
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfoundUnknownSuffix() {
        client.write("GET /sample_template.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }


    /**
     * When we POST some data unauthorized, we should receive that message
     */
    @Test
    fun testShouldGetUnauthorizedResponseAfterPost() {
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.UNAUTHORIZED, result.statusCode)
    }

    /**
     * When we POST some data, we should receive a success message back
     */
    @Test
    fun testShouldGetSuccessResponseAfterPost() {
        au.getUserForSessionBehavior = { NO_USER }
        client.write("POST /${NamedPaths.REGISTER.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        val body = "${RegisterAPI.Elements.EMPLOYEE_INPUT.elemName}=1&${RegisterAPI.Elements.USERNAME_INPUT.elemName}=abcdef&${RegisterAPI.Elements.PASSWORD_INPUT.elemName}=password12345"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)
        val result: AnalyzedHttpData = parseHttpMessage(client, au)
        assertEquals(StatusCode.OK, result.statusCode)
    }

    /**
     * When we POST some data that lacks all the types needed, get a 500 error
     */
    @Test
    fun testShouldGetInternalServerError() {
        au.getUserForSessionBehavior = { DEFAULT_USER }
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        val body = "project_entry=1&time_entry=2"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val result: AnalyzedHttpData = parseHttpMessage(client, au)

        assertEquals(StatusCode.INTERNAL_SERVER_ERROR, result.statusCode)
    }

    /**
     * If the body doesn't have properly URL formed text. Like not including a key
     * and a value separated by an =
     */
    @Test
    fun testShouldGetInternalServerError_improperlyFormedBody() {
        client.write("POST /${NamedPaths.ENTER_TIME.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        val body = "test foo bar"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.INTERNAL_SERVER_ERROR, result.statusCode)
    }

    /**
     * If we as client are connected but then close the connection from our side,
     * we should see a CLIENT_CLOSED_CONNECTION remark
     */
    @Test
    fun testShouldIndicateClientClosedConnection() {
        client.socket.shutdownOutput()

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(Verb.CLIENT_CLOSED_CONNECTION, result.verb)
    }

    /**
     * On some pages, like register and login, you are *supposed* to be
     * unauthenticated to post to them.  If you *are* authenticated and
     * post to those pages, you should get redirected to the authenticated
     * homepage
     */
    @Test
    fun testShouldGetRedirectedWhenPostingAuthAndRequireUnAuth() {
        au.getUserForSessionBehavior = { DEFAULT_USER }
        client.write("POST /${NamedPaths.LOGIN.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        val body = "${LoginAPI.Elements.USERNAME_INPUT.elemName}=alice&${LoginAPI.Elements.PASSWORD_INPUT.elemName}=password12345"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
    }

    /**
     * This is to try out a new client factory, so we send requests
     * with valid content and valid protocol more easily.
     *
     * Question: why does it take 6 seconds to run this 100 thousand times?
     */
    @Test
    fun testWithValidClient_LoginPage_PERFORMANCE() {
        // so we don't see spam
        logSettings[LogTypes.DEBUG] = false
        val headers = listOf("Connection: keep-alive")
        val body = mapOf(
                LoginAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
                LoginAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val myClient = Client.make(Verb.POST, NamedPaths.LOGIN.path, headers, body, au)

        val (time, _) = getTime {
            for (i in 1..100) {
                myClient.send()
                val result = myClient.read()

                assertEquals(StatusCode.OK, result.statusCode)
            }
        }
        println("Time was $time")
        // turn logging back on for other tests
        logSettings[LogTypes.DEBUG] = true
    }

    /**
     * I used this to see just how fast the server ran.  Able to get
     * 25,000 requests per second on 12/26/2020
     */
    @Test
    fun testHomepage_PERFORMANCE() {
        // so we don't see spam
        logSettings[LogTypes.DEBUG] = false
        val (time, _) = getTime {
            val threadList = (1..8).map {  makeClientThreadRepeatedRequestsHomepage(10) }
            threadList.forEach { it.join() }
        }
        println("Time was $time")
        // turn logging back on for other tests
        logSettings[LogTypes.DEBUG] = true
    }

    /**
     * I used this to see just how fast the server ran.  Able to get
     * 25,000 requests per second on 12/26/2020
     */
    @Test
    fun testEnterTime_PERFORMANCE() {
        // so we don't see spam
        logSettings[LogTypes.DEBUG] = false
        val (time, _) = getTime {
            val threadList = (1..8).map {  makeClientThreadRepeatedTimeEntries(10) }
            threadList.forEach { it.join() }
        }
        println("Time was $time")
        // turn logging back on for other tests
        logSettings[LogTypes.DEBUG] = true
    }

    /**
     * Simply GETs from the homepage many times
     */
    private fun makeClientThreadRepeatedRequestsHomepage(numRequests : Int): Thread {
        return thread {
            val client =
                Client.make(Verb.GET, NamedPaths.HOMEPAGE.path, listOf("Connection: keep-alive"), authUtilities = au)
            for (i in 1..numRequests) {
                client.send()
                val result = client.read()
                assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }

    /**
     * Enters time for a user on many days
     */
    private fun makeClientThreadRepeatedTimeEntries(numRequests : Int): Thread {
        return thread {

            val client =
                Client.make(
                    Verb.POST,
                    NamedPaths.ENTER_TIME.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=abc123"),
                    authUtilities = au)
            for (i in 1..numRequests) {
                val data = mapOf(
                    EnterTimeAPI.Elements.DATE_INPUT.elemName to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + i / 100).stringValue,
                    EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "some details go here",
                    EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                    EnterTimeAPI.Elements.TIME_INPUT.elemName to "1",
                )
                val clientWithData = client.addPostData(data)
                clientWithData.send()
                val result = clientWithData.read()
                assertEquals(StatusCode.OK, result.statusCode)
            }
        }
    }

}

class Client(private val socketWrapper: SocketWrapper, val data : String, val au: IAuthenticationUtilities, val path: String = "", private val headers: String = "") {

    fun send() {
        socketWrapper.write(data)
    }

    fun read() : AnalyzedHttpData {
        return parseHttpMessage(socketWrapper, au)
    }

    fun addPostData(body: Map<String, String>) : Client {
        val bodyString = body.map{ it.key + "=" + encode(it.value) }.joinToString("&")
        val data =  "${Verb.POST} /$path HTTP/1.1$CRLF" + "Content-Length: ${bodyString.length}$CRLF" + headers + CRLF + CRLF + bodyString
        return Client(this.socketWrapper, data = data, au)
    }

    companion object {

        fun make(
            verb : Verb,
            path : String,
            headers : List<String>? = null,
            body : Map<String,String>? = null,
            authUtilities: IAuthenticationUtilities = FakeAuthenticationUtilities()
        ) : Client {
            val clientSocket = Socket("localhost", 12345)
            val bodyString = body?.map{ it.key + "=" + encode(it.value) }?.joinToString("&") ?: ""
            val headersString = headers?.joinToString(CRLF) ?: ""

            val data = when (verb) {
                Verb.GET -> "${verb.name} /$path HTTP/1.1$CRLF" + headersString + CRLF + CRLF
                Verb.POST -> "${verb.name} /$path HTTP/1.1$CRLF" + "Content-Length: ${bodyString.length}$CRLF" + headersString + CRLF + CRLF + bodyString
                else -> throw IllegalArgumentException("unexpected Verb")
            }

            return Client(SocketWrapper(clientSocket, "client"), data, authUtilities, path, headersString)
        }

    }
}