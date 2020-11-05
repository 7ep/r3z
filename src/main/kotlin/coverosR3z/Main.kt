package coverosR3z

import coverosR3z.logging.logInfo
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.server.IOHolder
import coverosR3z.server.ServerUtilities
import java.net.ServerSocket

fun main() {
    // handle the GET
    val halfOpenServerSocket = ServerSocket(8080)
    val pmd = PureMemoryDatabase()
    // Following line runs when we connect with the browser
    while (true) {
        logInfo("waiting for socket connection")
        val serverSocket = halfOpenServerSocket.accept()
        logInfo("client from ${serverSocket.inetAddress?.hostAddress} has connected")
        val server = IOHolder(serverSocket)
        val su = ServerUtilities(server, pmd)
        su.serverHandleRequest()
    }
}