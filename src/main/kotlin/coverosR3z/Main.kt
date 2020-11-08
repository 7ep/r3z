package coverosR3z

import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.logging.logInfo
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.server.SocketWrapper
import coverosR3z.server.ServerUtilities
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import java.net.ServerSocket

fun main() {
    val halfOpenServerSocket = ServerSocket(8080)
    val pmd = PureMemoryDatabase()
    val cu = CurrentUser(SYSTEM_USER)
    val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), cu)
    val au = AuthenticationUtilities(AuthenticationPersistence(pmd))
    while (true) {
        logInfo("waiting for socket connection")
        val serverSocket = halfOpenServerSocket.accept()
        logInfo("client from ${serverSocket.inetAddress?.hostAddress} has connected")
        val server = SocketWrapper(serverSocket)
        val su = ServerUtilities(server, au, tru)
        su.serverHandleRequest()
    }
}