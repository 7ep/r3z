package coverosR3z

import coverosR3z.server.IOHolder
import coverosR3z.server.ServerUtilities
import java.net.ServerSocket

fun main(args: Array<String>) {
    // handle the GET
    val halfOpenServerSocket = ServerSocket(8080)
    // Following line runs when we connect with the browser
    while (true) {
        val serverSocket = halfOpenServerSocket.accept()
        val server = IOHolder(serverSocket)
        val su = ServerUtilities(server)
        su.serverHandleRequest()
    }
}