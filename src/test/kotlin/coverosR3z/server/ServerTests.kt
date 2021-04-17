package coverosR3z.server

import coverosR3z.config.utility.SystemOptions
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.misc.*
import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.FileReader.Companion.read
import coverosR3z.misc.utility.encode
import coverosR3z.misc.utility.getTime
import coverosR3z.misc.utility.toStr
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.StatusCode
import coverosR3z.server.types.Verb
import coverosR3z.server.utility.CRLF
import coverosR3z.server.utility.SocketWrapper
import coverosR3z.server.utility.parseHttpMessageAsClient
import coverosR3z.system.utility.FullSystem
import coverosR3z.timerecording.api.EnterTimeAPI
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Bear in mind this set of tests is to focus on the server functionality,
 * and *not* the underlying business code / database code.  That's why it
 * is fine to use fakes for the business code.  If you want to see
 * the server running with everything real, see [ServerPerformanceTests]
 */
class ServerTests {

    private lateinit var client : SocketWrapper

    @Before
    fun init() {
        // following is only used for ssl tests
        // note: the keystore is required by the ssl server.  See [SSLServer.init]
        val props = System.getProperties()
        props.setProperty("javax.net.ssl.trustStore", "src/test/resources/certs/truststore")
        props.setProperty("javax.net.ssl.trustStorePassword", "passphrase")

        val clientSocket = Socket("localhost", port)
        client = SocketWrapper(clientSocket, "client")
    }


    companion object {

        const val port = 2000
        const val specialPort = 2001
        const val sslTestPort = port + 443
        private lateinit var fs : FullSystem

        @JvmStatic
        @BeforeClass
        fun initServer() {
            fs = startFullSystem()
        }

        @JvmStatic
        @AfterClass
        fun stopServer() {
            logImperative("stopping server")
            fs.shutdown()
        }

        /**
         * The typical server startup, used by most tests in this file
         */
        private fun startFullSystem() = FullSystem.startSystem(
            SystemOptions(
                port = port,
                sslPort = sslTestPort,
                dbDirectory = "build/db/servertests",

                // not really checking security here, this keeps it simpler
                allowInsecure = true
            )
        )

    }

    /**
     * If we try something and are unauthenticated,
     * receive a 401 error page
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
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
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGet200Response() {
        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 303 SEE OTHER", statusline)
    }

    /**
     * If the client asks for a file, give it
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetFileResponse() {
        client.write("GET /sample.html HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.html")!!), result.data.rawData)
    }

    /**
     * If the client asks for a file, give it
     * CSS edition
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetFileResponse_CSS() {
        client.write("GET /sample.css HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.css")!!), result.data.rawData)
    }

    /**
     * If the client asks for a file, give it
     * JS edition
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetFileResponse_JS() {
        client.write("GET /sample.js HTTP/1.1$CRLF$CRLF")

        val result = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.OK, result.statusCode)
        assertEquals(toStr(read("static/sample.js")!!), result.data.rawData)
    }

    /**
     * Action for an invalid request
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldParseMultipleClientRequestTypes_BadRequest() {
        client.write("FOO /test.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.BAD_REQUEST, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server doesn't have?
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfound() {
        client.write("GET /doesnotexist.html HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }

    /**
     * What should the server return if we ask for something
     * the server does have, but it's not a suffix we recognize?
     * See [coverosR3z.server.utility.StaticFilesUtilities.Companion.loadStaticFilesToCache]
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetHtmlFileResponseFromServer_unfoundUnknownSuffix() {
        client.write("GET /sample_template.utl HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.NOT_FOUND, result.statusCode)
    }


    /**
     * When we POST some data unauthorized, we should receive that message
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetUnauthorizedResponseAfterPost() {
        client.write("POST /${EnterTimeAPI.path} HTTP/1.1$CRLF$CRLF")

        val result: AnalyzedHttpData = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.UNAUTHORIZED, result.statusCode)
    }

    /**
     * If the body doesn't have properly URL formed text. Like not including a key
     * and a value separated by an =
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldGetInternalServerError_improperlyFormedBody() {
        client.write("POST /${EnterTimeAPI.path} HTTP/1.1$CRLF")
        client.write("Cookie: sessionId=abc123$CRLF")
        val body = "test foo bar"
        client.write("Content-Length: ${body.length}$CRLF$CRLF")
        client.write(body)

        val result: AnalyzedHttpData = parseHttpMessageAsClient(client, testLogger)

        assertEquals(StatusCode.INTERNAL_SERVER_ERROR, result.statusCode)
    }

    /**
     * If we as client are connected but then close the connection from our side,
     * we should see a CLIENT_CLOSED_CONNECTION remark
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldIndicateClientClosedConnection() {
        client.socket.shutdownOutput()

        val result: AnalyzedHttpData = parseHttpMessageAsClient(client, testLogger)

        assertEquals(Verb.CLIENT_CLOSED_CONNECTION, result.verb)
    }

    /**
     * I used this to see just how fast the server ran.  Able to get
     * 25,000 requests per second on 12/26/2020
     */
    @IntegrationTest(usesPort = true)
    @Category(PerformanceTestCategory::class)
    @Test
    fun testHomepage_PERFORMANCE() {
        val numberOfThreads = 10
        val numberOfRequests = 300

        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

        // so we don't see spam
        fs.logger.turnOffAllLogging()
        val (time, _) = getTime {
            val threadList = (1..numberOfThreads).map {  cachedThreadPool.submit(makeClientThreadRepeatedRequestsHomepage(numberOfRequests, port)) }
            threadList.forEach { it.get() }
        }
        println("Time was $time")
        File("${granularPerfArchiveDirectory}testHomepage_PERFORMANCE")
            .appendText("${Date.now().stringValue}\tnumberOfThreads: $numberOfThreads\tnumberOfRequests: $numberOfRequests\ttime: $time\n")

        // turn logging back on for other tests
        fs.logger.resetLogSettingsToDefault()
    }

    /**
     * If we ask for the homepage on a secure server it will succeed
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testSecureEndpoint() {
        val sslClientSocket = SSLSocketFactory.getDefault().createSocket("localhost", sslTestPort) as SSLSocket
        client = SocketWrapper(sslClientSocket, "client")
        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 303 SEE OTHER", statusline)
    }

    /**
     * If we come in on the insecure endpoint, we should get
     * redirected to the secure endpoint
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testSecureEndpoint_Redirect() {
        val specialFS = startSpecialFullSystem(allowInsecure = false)
        val specialClientSocket = Socket("localhost", specialPort)
        val specialClient = SocketWrapper(specialClientSocket, "client")

        specialClient.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = specialClient.readLine()

        assertEquals("HTTP/1.1 303 SEE OTHER", statusline)
        specialFS.shutdown()
    }

    /**
     * If we ask for the homepage on a secure server,
     * and we provide a keystore and password in the system properties,
     * we'll succeed
     */
    @IntegrationTest(usesPort = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testSecureEndpoint_CheckSystemProperties() {
        val props = System.getProperties()
        props.setProperty("javax.net.ssl.keyStore", "src/main/resources/certs/keystore")
        props.setProperty("javax.net.ssl.keyStorePassword", "passphrase")
        val specialFS = startSpecialFullSystem()
        val sslClientSocket = SSLSocketFactory.getDefault().createSocket("localhost", sslTestPort) as SSLSocket
        client = SocketWrapper(sslClientSocket, "client")
        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        assertEquals("HTTP/1.1 303 SEE OTHER", statusline)
        specialFS.shutdown()
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
     * Simply GETs from the homepage many times
     */
    private fun makeClientThreadRepeatedRequestsHomepage(numRequests : Int, port : Int): Thread {
        return Thread {
            val client =
                Client.make(Verb.GET, HomepageAPI.path, listOf("Connection: keep-alive"), port = port)
            for (i in 1..numRequests) {
                client.send()
                val result = client.read()
                assertEquals(StatusCode.SEE_OTHER, result.statusCode)
            }
        }
    }

    /**
     * Similar to [startFullSystem] but allows individual tests to start their
     * own customized system for certain unusual configurations
     */
    private fun startSpecialFullSystem(allowInsecure: Boolean = true) = FullSystem.startSystem(
        SystemOptions(
            port = specialPort,
            sslPort = specialPort + 443,
            dbDirectory = null,

            // not really checking security here, this keeps it simpler
            allowInsecure = allowInsecure
        )
    )

}

class Client(private val socketWrapper: SocketWrapper, val data : String, val path: String = "", private val headers: String = "") {

    fun send() {
        socketWrapper.write(data)
    }

    fun read() : AnalyzedHttpData {
        return parseHttpMessageAsClient(socketWrapper, testLogger)
    }

    fun addPostData(body: PostBodyData) : Client {
        val bodyString = body.mapping.map{ it.key + "=" + encode(it.value) }.joinToString("&")
        val data =  "${Verb.POST} /$path HTTP/1.1$CRLF" + "Content-Length: ${bodyString.length}$CRLF" + headers + CRLF + CRLF + bodyString
        return Client(this.socketWrapper, data = data)
    }

    companion object {

        fun make(
            verb : Verb,
            path : String,
            headers : List<String>? = null,
            body : Map<String,String>? = null,
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

            return Client(SocketWrapper(clientSocket, "client"), data, path, headersString)
        }

    }
}
