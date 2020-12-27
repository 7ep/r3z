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

    /**
     * If we run main with a particular port number,
     * it should indicate that during startup
     */
    @Test
    fun testMain() {
        // starts the server
        thread{main(arrayOf("-p","54321"))}

        // Give the system sufficient time to startup and set
        // its shutdown hooks, so when we shut it down it will
        // try to do so cleanly
        Thread.sleep(500)
    }

}