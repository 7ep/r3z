package coverosR3z

import coverosR3z.server.Server
import org.junit.Test
import kotlin.concurrent.thread

class MainTests {

    @Test
    fun testMain() {
        thread{main(arrayOf("12345"))}
        Thread.sleep(200)
        Server.halfOpenServerSocket.close()
    }
}