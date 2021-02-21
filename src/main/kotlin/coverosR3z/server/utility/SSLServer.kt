package coverosR3z.server.utility

import coverosR3z.FullSystem
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.logging.getCurrentMillis
import coverosR3z.misc.types.DateTime
import coverosR3z.misc.utility.FileReader
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.ServerObjects
import java.util.concurrent.ExecutorService

import java.security.KeyStore

import java.security.SecureRandom
import javax.net.ssl.*


class SSLServer(
    sslPort: Int,
    private val executorService: ExecutorService,
    private val businessObjects: BusinessCode,
    private val serverObjects: ServerObjects,
    private val fullSystem: FullSystem
) {

    // create the secure SSL socket
    val sslHalfOpenServerSocket : SSLServerSocket

    init {
        sslHalfOpenServerSocket = createSslSocketFactory(sslPort)
        logImperative("SSL server is ready at https://localhost:$sslPort.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")
    }


    fun createSecureServerThread() : Thread {
        return ServerUtilities.createServerThread(executorService, fullSystem, sslHalfOpenServerSocket, businessObjects, serverObjects)
    }


    /**
     * This will create an SSLSocket for us.  The complicated bit is the
     * certificates.  If it finds them set in the system properties, it will
     * use those.  If not, it defaults to using a self-signed cert we keep with us.
     */
    private fun createSslSocketFactory(sslPort: Int): SSLServerSocket {
        /**
         * If we find the keystore and pass in the system properties
         */
        val hasKeystoreAndPasswordOnSystemProperties = checkSystemPropertiesForKeystore()

        return if (hasKeystoreAndPasswordOnSystemProperties) {
            logImperative("Using keystore and password provided in system properties")
            SSLServerSocketFactory.getDefault().createServerSocket(sslPort) as SSLServerSocket
        } else {
            logImperative("Using the default (self-signed / testing-only) certificate")
            createSslSocketWithInternalKeystore(sslPort)
        }
    }

    /**
     * Look into the system properties to see whether values have been
     * set for the keystore and keystorePassword keys.
     *
     * the key for keystore is:  javax.net.ssl.keyStore
     * the key for keystorePassword is: javax.net.ssl.keyStorePassword
     *
     * It's smart, if you are creating a server that will run
     * with a genuine signed certificate, to have those files
     * stored somewhere and then set these system properties.  That
     * way, it's a characteristic of a particular server - it's not
     * needed to bundle the certificate with the actual server in
     * any way.
     *
     * We *do* bundle a cert, but it's for testing and is self-signed.
     */
    private fun checkSystemPropertiesForKeystore(): Boolean {
        val props = System.getProperties()

        // get the directory to the keystore from a system property
        val keystore = props.getProperty("javax.net.ssl.keyStore")
        if (keystore == null) {
            logImperative("Keystore system property was not set")
        }

        // get the password to that keystore from a system property
        val keystorePassword = props.getProperty("javax.net.ssl.keyStorePassword")
        if (keystorePassword == null) {
            logImperative("keystorePassword system property was not set")
        }

        return keystore != null && keystorePassword != null
    }

    /**
     * Creates an SSL Socket using a keystore we have internally, meant
     * for ssl testing and other situations where a self-signed keystore will suffice
     *
     * It's so convoluted just because the interface to this stuff on the standard library
     * doesn't expect the typical user to read a keystore programmatically.  Realistic use
     * of SSL sockets typically (strongly) suggests using a real key signed by a certificate
     * authority.
     */
    private fun createSslSocketWithInternalKeystore(sslPort: Int): SSLServerSocket {
        val keyPassword = "passphrase".toCharArray()
        val keystoreFile = FileReader.readAsStream("certs/keystore")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(keystoreFile, keyPassword)

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, keyPassword)

        val keyManagers = keyManagerFactory.keyManagers

        val sslContext: SSLContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(keyManagers, null, SecureRandom())

        val socketFactory: SSLServerSocketFactory = sslContext.serverSocketFactory
        return socketFactory.createServerSocket(sslPort) as SSLServerSocket
    }

}