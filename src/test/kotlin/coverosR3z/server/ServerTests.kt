package coverosR3z.server

import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.LoginAPI
import coverosR3z.authentication.RegisterAPI
import coverosR3z.domainobjects.NO_USER
import coverosR3z.exceptions.ServerOptionsException
import coverosR3z.misc.FileReader.Companion.read
import coverosR3z.misc.toStr
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.net.Socket
import kotlin.concurrent.thread

class ServerTests {

    private lateinit var client : SocketWrapper

    companion object {

        private lateinit var serverObject : Server
        private lateinit var au : FakeAuthenticationUtilities

        @JvmStatic @BeforeClass
        fun beforeClass() {
            serverObject = Server(12345)
            au = FakeAuthenticationUtilities()
            thread { serverObject.startServer(au) }
        }

        @JvmStatic @AfterClass
        fun afterClass() {
            Server.halfOpenServerSocket.close()
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
        val serverOptions = Server.extractOptions(arrayOf())
        assertEquals(ServerOptions(), serverOptions)
    }

    @Test
    fun testShouldParseOptions_Port() {
        val serverOptions = Server.extractOptions(arrayOf("-p","54321"))
        assertEquals(ServerOptions(54321), serverOptions)
    }

    @Test
    fun testShouldParseOptions_PortAnotherValid() {
        val serverOptions = Server.extractOptions(arrayOf("-p","11111"))
        assertEquals(ServerOptions(11111), serverOptions)
    }

    @Test
    fun testShouldParseOptions_weirdDatabaseDirectory() {
        val serverOptions = Server.extractOptions(arrayOf("-d-p1024"))
        assertEquals(ServerOptions(dbDirectory = "-p1024"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_NoDiskPersistenceOption() {
        val serverOptions = Server.extractOptions(arrayOf("--no-disk-persistence"))
        assertEquals(ServerOptions(12345, null), serverOptions)
    }

    @Test
    fun testShouldParseOptions_PortNoSpace() {
        val serverOptions = Server.extractOptions(arrayOf("-p54321"))
        assertEquals(ServerOptions(54321), serverOptions)
    }

    /**
     * There is no -a option currently
     */
    @Test
    fun testShouldParseOptions_UnrecognizedOptions() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-p", "54321", "-d", "2321", "-a",  "213123"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Unrecognized options"))
    }

    /**
     * The port provided must be an integer between 0 and 65535
     * It will probably complain though if you run this below 1024
     * as non-root (below 1024, you need admin access on the machine, typically)
     */
    @Test
    fun testShouldParseOptions_badPort_nonInteger() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-pabc123"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Must be able to parse abc123 as integer"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_negativeInteger() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-p", "-1"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: -p -1"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_zero() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-p", "0"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: -p 0"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_above65535() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-p", "65536"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: -p 65536"))
    }

    /**
     * If we provide no value to the port, complain
     */
    @Test
    fun testShouldParseOptions_badPort_empty() {
        val ex = assertThrows(ServerOptionsException::class.java) {
            Server.extractOptions(arrayOf("-p", "-d", "db"))
        }
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Must be able to parse -d as integer"))
    }

    @Test
    fun testShouldParseOptions_DatabaseDirectory() {
        val serverOptions = Server.extractOptions(arrayOf("-d", "build/db"))
        assertEquals("build/db", serverOptions.dbDirectory)
    }

    @Test
    fun testShouldParseOptions_BadPort_TooMany() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-p", "22", "-p", "33"))}
        assertTrue(ex.message!!.contains("Multiple port values were provided"))
    }

    @Test
    fun testShouldParseOptions_DatabaseDirectoryNoSpace() {
        val serverOptions = Server.extractOptions(arrayOf("-dbuild/db"))
        assertEquals("build/db", serverOptions.dbDirectory)
    }

    @Test
    fun testShouldParseOptions_badDatabaseDirectory_Empty() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-d"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The directory option was provided without a directory value"))
    }

    @Test
    fun testShouldParseOptions_badDatabaseDirectory_EmptyAlternate() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-d",  "-p1024"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The directory option was provided without a directory value"))
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
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("--no-disk-persistence", "-dbuild/db"))}
        val expected = "If you're setting the noDiskPersistence option and also a database directory, you're very foolish"
        assertTrue("Message needs to match expected; yours was:\n${ex.message}", ex.message!!.contains(expected))
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation1() {
        val serverOptions = Server.extractOptions(arrayOf("-p54321", "-dbuild/db"))
        assertEquals(ServerOptions(54321, "build/db"),serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation2() {
        val serverOptions = Server.extractOptions(arrayOf("-dbuild/db", "-p54321"))
        assertEquals(ServerOptions(54321, "build/db"),serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation3() {
        val serverOptions = Server.extractOptions(arrayOf("-dbuild/db", "-p", "54321"))
        assertEquals(ServerOptions(54321, "build/db"),serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation4() {
        val serverOptions = Server.extractOptions(arrayOf("-d", "build/db", "-p", "54321"))
        assertEquals(ServerOptions(54321, "build/db"),serverOptions)
    }

    /**
     * If the user asks for help with -h or -?, provide
     * an explanation of the app options
     */
    @Test
    fun testShouldHelpUser() {
        val ex = assertThrows(ServerOptionsException::class.java) {Server.extractOptions(arrayOf("-h"))}
        assertEquals(Server.fullHelpMessage, ex.message!!)
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
        assertEquals(toStr(read("sample.html")!!), result.rawData)
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
        assertEquals(toStr(read("sample.css")!!), result.rawData)
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
        assertEquals(toStr(read("sample.js")!!), result.rawData)
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

        val result: AnalyzedHttpData = parseHttpMessage(client, FakeAuthenticationUtilities())

        assertEquals(StatusCode.INTERNAL_SERVER_ERROR, result.statusCode)
    }

    /**
     * If the body doesn't have properly URL formed text. Like not including a key
     * and a value separated by an =
     */
    @Test
    fun testShouldGetInternalServerError_improperlyFormedBody() {
        au.getUserForSessionBehavior = { DEFAULT_USER }
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

}