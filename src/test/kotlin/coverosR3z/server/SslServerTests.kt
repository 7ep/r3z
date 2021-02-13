package coverosR3z.server

import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.logging.logImperative
import coverosR3z.misc.IntegrationTest
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.CRLF
import coverosR3z.server.utility.Server
import coverosR3z.server.utility.SocketWrapper
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.properties.Delegates

class SslServerTests {
    private lateinit var client : SocketWrapper
    private var testPort by Delegates.notNull<Int>()
    private var sslTestPort by Delegates.notNull<Int>()
    private lateinit var serverObject : Server
    private val au = FakeAuthenticationUtilities()
    private val tru = FakeTimeRecordingUtilities()


    /**
     * If we ask for the homepage on a secure server, we'll get a 200 OK
     */
    @IntegrationTest(usesPort = true)
    @Test
    fun testShouldGet200Response_Secure() {
        client.write("GET /homepage HTTP/1.1$CRLF$CRLF")

        val statusline = client.readLine()

        Assert.assertEquals("HTTP/1.1 200 OK", statusline)
    }

    @Before
    fun initServer() {
        val props = System.getProperties()
        props.setProperty("javax.net.ssl.keyStore", "src/test/resources/certs/keystore")
        props.setProperty("javax.net.ssl.keyStorePassword", "passphrase")
        props.setProperty("javax.net.ssl.trustStore", "src/test/resources/certs/truststore")
        props.setProperty("javax.net.ssl.trustStorePassword", "passphrase")

        testPort = port.getAndIncrement()
        sslTestPort = 12443
        val sleeptime = 50L
        serverObject = Server(testPort, 12443)

        // start the unencrypted server
        val serverThread = serverObject.createServerThread(BusinessCode(tru, au))
        val executor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
        executor.submit(serverThread)

        // start the SSL server
        val secureServerThread = serverObject.createSecureServerThread(BusinessCode(tru, au))
        val sslExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
        sslExecutor.submit(secureServerThread)

        // wait for system ready
        while (!serverObject.systemReady) {
            logImperative("System is not ready, sleeping for $sleeptime milliseconds")
            Thread.sleep(sleeptime)
        }

        // create a SSL client (this requires system properties to be set, since
        // it won't connect to the server unless it is trusted.  That is why we
        // need both tht keystore and truststore, and their associated passwords
        val sslClientSocket = SSLSocketFactory.getDefault().createSocket("localhost", sslTestPort) as SSLSocket
        sslClientSocket.enabledProtocols = arrayOf("TLSv1.3")
        sslClientSocket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
        client = SocketWrapper(sslClientSocket, "client")
    }

    @After
    fun stopServer() {
        logImperative("stopping server")
        serverObject.halfOpenServerSocket.close()
    }

    companion object {
        val port = AtomicInteger(2080)
    }

}