package coverosR3z

import coverosR3z.server.Server
import org.junit.After
import org.junit.Test
import kotlin.concurrent.thread

/**
 * Due to the entirely-stateful and threaded nature of these tests,
 * these tests are here as experiments to see how the system behaves,
 * during which time the experimenter observes the logs of what is
 * happening, rather than as typical (asserts).
 *
 * Try to avoid adding more tests here.  It is better to test the code
 * underneath with less-stateful and easier tests.
 */
class MainTests {

    @After
    fun cleanup() {
        val tryingToClose = thread {
            while (true) {
                try {
                    Server.halfOpenServerSocket.close()
                    break
                } catch (ex: UninitializedPropertyAccessException) {
                    Thread.sleep(10)
                }
            }
        }
        tryingToClose.join()
    }

    /**
     * If we run main with a particular port number,
     * it should indicate that during startup
     */
    @Test
    fun testMain() {
        val data = arrayOf("-p 54321")

        // first thread starts the server
        thread{main(data)}
    }


}