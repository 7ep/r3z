package coverosR3z.server

import coverosR3z.config.utility.SystemOptions
import coverosR3z.config.utility.SystemOptions.Companion.extractOptions
import coverosR3z.config.utility.SystemOptions.Companion.fullHelpMessage
import coverosR3z.misc.exceptions.SystemOptionsException
import org.junit.Assert.*
import org.junit.Test

class SystemOptionTests {


    /**
     * If we provide no options, it defaults to 12345 and a database directory of "db"
     */
    @Test
    fun testShouldParsePortFromCLI_nothingProvided() {
        val serverOptions = extractOptions(arrayOf())
        assertEquals(SystemOptions(), serverOptions)
    }

    @Test
    fun testShouldParseOptions_Port() {
        val serverOptions = extractOptions(arrayOf("-p","54321"))
        assertEquals(SystemOptions(54321), serverOptions)
    }

    @Test
    fun testShouldParseOptions_PortAnotherValid() {
        val serverOptions = extractOptions(arrayOf("-p","11111"))
        assertEquals(SystemOptions(11111), serverOptions)
    }

    @Test
    fun testShouldParseOptions_weirdDatabaseDirectory() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-d-p1024"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -d option was provided without a value: -d-p1024"))
    }

    @Test
    fun testShouldParseOptions_NoDiskPersistenceOption() {
        val serverOptions = extractOptions(arrayOf("--no-disk-persistence"))
        assertEquals(SystemOptions(12345, dbDirectory = null), serverOptions)
    }

    @Test
    fun testShouldParseOptions_PortNoSpace() {
        val serverOptions = extractOptions(arrayOf("-p54321"))
        assertEquals(SystemOptions(54321), serverOptions)
    }

    /**
     * There is no -a option currently
     */
    @Test
    fun testShouldParseOptions_UnrecognizedOptions() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-p", "54321", "-d", "2321", "-a",  "213123"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("argument not recognized: -a"))
    }

    /**
     * The port provided must be an integer between 0 and 65535
     * It will probably complain though if you run this below 1024
     * as non-root (below 1024, you need admin access on the machine, typically)
     */
    @Test
    fun testShouldParseOptions_badPort_nonInteger() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-pabc123"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Must be able to parse abc123 as integer"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_negativeInteger() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-p", "-1"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -p option was provided without a value: -p -1"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_zero() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-p", "0"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: 0"))
    }

    /**
     * See [testShouldParseOptions_badPort_negativeInteger]
     */
    @Test
    fun testShouldParseOptions_badPort_above65535() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-p", "65536"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("port number was out of range.  Range is 1-65535.  Your input was: 65536"))
    }

    /**
     * If we provide no value to the port, complain
     */
    @Test
    fun testShouldParseOptions_badPort_empty() {
        val ex = assertThrows(SystemOptionsException::class.java) {
            extractOptions(arrayOf("-p", "-d", "db"))
        }
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -p option was provided without a value: -p -d db"))
    }

    @Test
    fun testShouldParseOptions_DatabaseDirectory() {
        val serverOptions = extractOptions(arrayOf("-d", "build/db"))
        assertEquals(SystemOptions(dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_BadPort_TooMany() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-p", "22", "-p", "33"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("Duplicate options were provided."))
    }

    @Test
    fun testShouldParseOptions_DatabaseDirectoryNoSpace() {
        val serverOptions = extractOptions(arrayOf("-dbuild/db"))
        assertEquals(SystemOptions(dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_badDatabaseDirectory_Empty() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-d"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -d option was provided without a value: -d"))
    }

    @Test
    fun testShouldParseOptions_badDatabaseDirectory_EmptyAlternate() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-d",  "-p1024"))}
        assertTrue("Message needs to match expected; your message was:\n${ex.message}", ex.message!!.contains("The -d option was provided without a value: -d -p1024"))
    }

    /**
     * This is valid but not great
     *
     * This is likely a typo by the user, they probably meant to set
     * multiple parameters, but on the other hand the database
     * option needs a value after, so if our user typed it in
     * like: r3z -d-p1024, yeah, they'll get a directory of -p1024
     * which looks nuts, but we'll allow it.
     *
     * By the way, if the user *does* put in something that the
     * operating system won't allow, they will get a complaint,
     * an exception that stems from the File.write command
     */


    @Test
    fun testShouldParseOptions_multipleOptionsInvalid() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("--no-disk-persistence", "-dbuild/db"))}
        val expected = "If you're setting the noDiskPersistence option and also a database directory, you're very foolish"
        assertTrue("Message needs to match expected; yours was:\n${ex.message}", ex.message!!.contains(expected))
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation1() {
        val serverOptions = extractOptions(arrayOf("-p54321", "-dbuild/db"))
        assertEquals(SystemOptions(54321, dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation2() {
        val serverOptions = extractOptions(arrayOf("-dbuild/db", "-p54321"))
        assertEquals(SystemOptions(54321, dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation3() {
        val serverOptions = extractOptions(arrayOf("-dbuild/db", "-p", "54321"))
        assertEquals(SystemOptions(54321, dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_multipleValidOptions_permutation4() {
        val serverOptions = extractOptions(arrayOf("-d", "build/db", "-p", "54321"))
        assertEquals(SystemOptions(54321, dbDirectory = "build/db/"), serverOptions)
    }

    @Test
    fun testShouldParseOptions_setAllLoggingOff() {
        val serverOptions = extractOptions(arrayOf("-d", "build/db", "-p", "54321", "--no-logging"))
        assertEquals(SystemOptions(54321, dbDirectory = "build/db/", allLoggingOff = true), serverOptions)
    }

    /**
     * If the user asks for help with -h or -?, provide
     * an explanation of the app options
     */
    @Test
    fun testShouldHelpUser() {
        val ex = assertThrows(SystemOptionsException::class.java) {extractOptions(arrayOf("-h"))}
        assertEquals(fullHelpMessage, ex.message!!.trimIndent())
    }

    /**
     * Allow a user to set the port for SSL communication
     */
    @Test
    fun testShouldSetSslPort() {
        val serverOptions = extractOptions(arrayOf("-s", "54321"))
        assertEquals(SystemOptions(sslPort = 54321), serverOptions)
    }

    @Test
    fun testShouldRedirectToSsl() {
        val serverOptions = extractOptions(arrayOf("--force-ssl"))
        assertEquals(SystemOptions(forceRedirectToSsl = true), serverOptions)
    }

}