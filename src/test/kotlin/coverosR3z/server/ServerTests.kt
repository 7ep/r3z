package coverosR3z.server

import coverosR3z.*
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.logging.LogConfig.logSettings
import coverosR3z.logging.LogTypes
import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.FileReader.Companion.read
import coverosR3z.misc.utility.encode
import coverosR3z.misc.utility.getTime
import coverosR3z.misc.utility.toStr
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.StatusCode
import coverosR3z.server.types.Verb
import coverosR3z.server.utility.CRLF
import coverosR3z.server.utility.Server
import coverosR3z.server.utility.SocketWrapper
import coverosR3z.server.utility.parseHttpMessage
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.api.EnterTimeAPI
import org.junit.*
import org.junit.Assert.assertEquals
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

    private lateinit var serverObject : Server
    private val au = FakeAuthenticationUtilities()
    private val tru = FakeTimeRecordingUtilities()

    private fun initServer(port : Int) {
        serverObject = Server(port)
        serverObject.startServer(BusinessCode(tru, au))
        val clientSocket = Socket("localhost", port)
        client = SocketWrapper(clientSocket, "client")
    }

    @After
    fun stopServer() {
        serverObject.halfOpenServerSocket.close()
    }

    /**
     * If we try something and are unauthenticated,
     * receive a 401 error page
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldReturnUnauthenticatedAs401Page() {
        val port = 6000
        initServer(port)

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
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGet200Response() {
        val port = 6002
        initServer(port)

        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 200 OK", statusline)
    }

    /**
     * If the client asks for a file, give it
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetFileResponse() {
        val port = 6003
        initServer(port)

        client.write("GET /sample.html HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.html")!!), result.rawData)
    }

    /**
     * If the client asks for a file, give it
     * CSS edition
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetFileResponse_CSS() {
        val port = 6004
        initServer(port)

        client.write("GET /sample.css HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.css")!!), result.rawData)
    }

    /**
     * If the client asks for a file, give it
     * JS edition
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetFileResponse_JS() {
        val port = 6005
        initServer(port)

        client.write("GET /sample.js HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.js")!!), result.rawData)
    }

    /**
     * Action for an invalid request
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldParseMultipleClientRequestTypes_BadRequest() {
        val port = 6006
        initServer(port)

        client.write("FOO /test.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.BAD_REQUEST, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        val port = 6007
        initServer(port)

        client.write("GET /doesnotexist.html HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server does have, but it's not a suffix we recognize?
     * See ServerUtilties.handleUnknownFiles
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfoundUnknownSuffix() {
        val port = 6008
        initServer(port)

        client.write("GET /sample_template.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }


    /**
     * When we POST some data unauthorized, we should receive that message
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetUnauthorizedResponseAfterPost() {
        val port = 6009
        initServer(port)

        client.write("POST /${EnterTimeAPI.path} HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.UNAUTHORIZED, result.statusCode)
    }

    /**
     * When we POST some data, we should receive a success message back
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetSuccessResponseAfterPost() {
        val port = 6010
        initServer(port)

        au.getUserForSessionBehavior = { NO_USER }
        client.write("POST /${RegisterAPI.path} HTTP/1.1$CRLF")
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
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetInternalServerError() {
        val port = 6011
        initServer(port)

        au.getUserForSessionBehavior = { DEFAULT_USER }
        client.write("POST /${EnterTimeAPI.path} HTTP/1.1$CRLF")
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
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetInternalServerError_improperlyFormedBody() {
        val port = 6012
        initServer(port)

        client.write("POST /${EnterTimeAPI.path} HTTP/1.1$CRLF")
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
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldIndicateClientClosedConnection() {
        val port = 6013
        initServer(port)

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
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGetRedirectedWhenPostingAuthAndRequireUnAuth() {
        val port = 6014
        initServer(port)

        au.getUserForSessionBehavior = { DEFAULT_USER }
        client.write("POST /${LoginAPI.path} HTTP/1.1$CRLF")
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
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testWithValidClient_LoginPage_PERFORMANCE() {
        val port = 6015
        initServer(port)

        // so we don't see spam
        logSettings[LogTypes.DEBUG] = false
        val headers = listOf("Connection: keep-alive")
        val body = mapOf(
                LoginAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
                LoginAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val myClient = Client.make(Verb.POST, LoginAPI.path, headers, body, au, port)

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
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testHomepage_PERFORMANCE() {
        val port = 6016
        initServer(port)

        // so we don't see spam
        logSettings[LogTypes.DEBUG] = false
        val (time, _) = getTime {
            val threadList = (1..8).map {  makeClientThreadRepeatedRequestsHomepage(10, port) }
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
    @IntegrationTest(usesPort = true)
    @PerformanceTest
    @Test
    fun testEnterTime_PERFORMANCE() {
        val port = 6017
        initServer(port)

        // so we don't see spam
        logSettings[LogTypes.DEBUG] = false
        val (time, _) = getTime {
            val threadList = (1..8).map {  makeClientThreadRepeatedTimeEntries(10, port) }
            threadList.forEach { it.join() }
        }
        println("Time was $time")
        // turn logging back on for other tests
        logSettings[LogTypes.DEBUG] = true
    }

    /**
     * Simply GETs from the homepage many times
     */
    private fun makeClientThreadRepeatedRequestsHomepage(numRequests : Int, port : Int): Thread {
        return thread {
            val client =
                Client.make(Verb.GET, HomepageAPI.path, listOf("Connection: keep-alive"), authUtilities = au, port = port)
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
    private fun makeClientThreadRepeatedTimeEntries(numRequests : Int, port : Int): Thread {
        return thread {

            val client =
                Client.make(
                    Verb.POST,
                    EnterTimeAPI.path,
                    listOf("Connection: keep-alive", "Cookie: sessionId=abc123"),
                    authUtilities = au,
                    port = port)
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
            authUtilities: IAuthenticationUtilities = FakeAuthenticationUtilities(),
            port : Int
        ) : Client {
            val clientSocket = Socket("localhost", port)
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