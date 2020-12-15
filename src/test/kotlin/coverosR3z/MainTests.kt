package coverosR3z

import coverosR3z.server.Server
import org.junit.Test
import kotlin.concurrent.thread

class MainTests {

    @Test
    fun testMain() {
        // first thread starts the server
        thread{main(arrayOf("12345"))}

        // second thread tries to stop the server when the server is stoppable
        // it loops trying to shut down the server, but initially the variable
        // won't be initialized.  It takes a second or two.  So this will
        // throw tons of [UninitializedPropertyAccessException] but we catch
        // them all and only allow the program to continue when it stops throwing.
        val closingThread = thread{
            while(true) {
                try {
                    Server.halfOpenServerSocket.close()
                    break
                } catch (ex: UninitializedPropertyAccessException) {
                    Thread.sleep(10)
                }
            }
        }

        // this line forces the test to wait here until
        // we have finished closing the socket.  Without this,
        // we just exit the test with the threads unfinished
        closingThread.join()
    }
}