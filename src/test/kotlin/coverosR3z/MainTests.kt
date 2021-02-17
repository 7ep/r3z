package coverosR3z

import coverosR3z.misc.IntegrationTest
import coverosR3z.misc.IntegrationTestCategory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.*
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
    @IntegrationTest(usesPort = true, usesDirectory = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testMain() {
        val dbPath = "build/db/maintest/"
        File(dbPath).deleteRecursively()

        val (previousOut, out) = captureSystemOut()

        // starts the server
        thread{main(arrayOf("-p","54321", "-d", dbPath))}

        // Give the system sufficient time to startup and set
        // its shutdown hooks, so when we shut it down it will
        // try to do so cleanly
        Thread.sleep(500)
        System.setOut(previousOut)

        val expectedResults = listOf(
            "IMPERATIVE: database directory is build/db/maintest/",
            "IMPERATIVE: directory build/db/maintest/1/ did not exist.  Returning null for the PureMemoryDatabase",
            "IMPERATIVE: No existing database found, building new database",
            "IMPERATIVE: Created new PureMemoryDatabase",
            "IMPERATIVE: Created the database directory at \"build/db/maintest/1/",
            "IMPERATIVE: Wrote the version of the database (1) to currentVersion.txt")

        compareInOrder(expectedResults, out.toString())
    }

    /**
     * So we can capture what the logging would send to the system out, we
     * will replace the regular out stream with something we control.
     * We will return both the streams, especially the old one (so we can
     * set it back the way it was after we're done with it)
     */
    private fun captureSystemOut(): Pair<PrintStream, ByteArrayOutputStream> {
        val previousOut = System.out
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        return Pair(previousOut, out)
    }

    /**
     * compares a list of strings against a string, to confirm that the list
     * appears in the string, in order.
     * @param s one giant block of text
     * @param stringList a list of strings.  We'll search for each one in the big block, in order.
     */
    private fun compareInOrder(stringList : List<String>, s: String) {
        var startIndex = 0
        for (string in stringList) {
            val foundIndex = s.indexOf(string, startIndex)
            startIndex += foundIndex - startIndex
            assertTrue("Must find $string in $s", foundIndex > 0)
        }
    }

}