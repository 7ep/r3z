package coverosR3z

import coverosR3z.server.SocketCommunication

/**
 * Entry point for the application.  KISS.
 */
fun main() {
    SocketCommunication(8080).startServer()
}