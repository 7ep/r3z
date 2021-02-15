package coverosR3z.server.utility

import coverosR3z.FullSystem
import coverosR3z.logging.Logger
import coverosR3z.logging.getCurrentMillis
import coverosR3z.misc.types.DateTime
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.ServerObjects
import coverosR3z.server.utility.ServerUtilities.Companion.processConnectedClient
import java.net.SocketException
import java.util.concurrent.ExecutorService
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory

class SSLServer(
    sslPort: Int,
    private val executorService: ExecutorService,
    private val serverObjects: ServerObjects,
    private val fullSystem: FullSystem
) {

    // create the secure SSL socket
    val sslHalfOpenServerSocket : SSLServerSocket

    init {
        // set up the secure server socket
        val props = System.getProperties()
        props.setProperty("javax.net.ssl.keyStore", "src/test/resources/certs/keystore")
        props.setProperty("javax.net.ssl.keyStorePassword", "passphrase")
        props.setProperty("javax.net.ssl.trustStore", "src/test/resources/certs/truststore")
        props.setProperty("javax.net.ssl.trustStorePassword", "passphrase")
        sslHalfOpenServerSocket = SSLServerSocketFactory.getDefault().createServerSocket(sslPort) as SSLServerSocket

        Logger.logImperative("SSL server is ready at https://localhost:$sslPort.  DateTime is ${DateTime(getCurrentMillis() / 1000)} in UTC")
    }

    fun createSecureServerThread(businessObjects : BusinessCode) : Thread {
        return Thread {
            try {
                while (true) {
                    fullSystem.logger.logTrace { "waiting for socket connection" }
                    val server = SocketWrapper(sslHalfOpenServerSocket.accept(), "server", fullSystem)
                    executorService.submit(Thread { processConnectedClient(
                        server,
                        businessObjects,
                        serverObjects,
                    ) })
                }
            } catch (ex: SocketException) {
                if (ex.message == "Interrupted function call: accept failed") {
                    Logger.logImperative("Server was shutdown while waiting on accept")
                }
            }
        }
    }

}