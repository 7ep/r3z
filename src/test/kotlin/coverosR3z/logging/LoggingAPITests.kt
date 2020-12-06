package coverosR3z.logging

import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.exceptions.InexactInputsException
import coverosR3z.server.PreparedResponseData
import coverosR3z.server.StatusCode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class LoggingAPITests {

    @Before
    fun init() {
        resetLogSettingsToDefault()
    }

    @After
    fun cleanup() {
        resetLogSettingsToDefault()
    }

    /**
     * Make sure it handles properly if we are authenticated
     */
    @Test
    fun testShouldPostLoggingPageIfAuthenticated() {
        val data = allTrue()
        val result : PreparedResponseData = handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(StatusCode.OK, result.statusCode)
    }

    /**
     * Should redirect to the homepage if unauthenticated
     */
    @Test
    fun testShouldNotPostLoggingPageIfUnauthenticated() {
        val result : PreparedResponseData = handlePOSTLogging(NO_USER, emptyMap())
        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
    }

    /**
     * If we
     */
    @Test
    fun testShouldChangeConfiguration_setAuditTrueOnly() {
        val data = allFalse(audit = "true")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(true, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setWarnTrueOnly() {
        val data = allFalse(warn = "true")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(true, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setDebugTrueOnly() {
        val data = allFalse(debug = "true")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(true, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setTraceTrueOnly() {
        val data = allFalse(trace = "true")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(true, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_allOn() {
        val data = allTrue()
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(true, logSettings[LogTypes.AUDIT])
        assertEquals(true, logSettings[LogTypes.WARN])
        assertEquals(true, logSettings[LogTypes.DEBUG])
        assertEquals(true, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_allOff() {
        val data = allFalse()
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    /**
     * We require four inputs from the user.  If any are missing, complain
     */
    @Test
    fun testShouldComplain_missingAudit() {
        val data = mapOf(
                LogTypes.WARN.name to "false",
                LogTypes.DEBUG.name to "true",
                LogTypes.TRACE.name to "false")
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals("expected keys: [AUDIT, DEBUG, WARN, TRACE]. received keys: [WARN, DEBUG, TRACE]", ex.message)
    }

    @Test
    fun testShouldComplain_missingWarn() {
        val data = mapOf(
                LogTypes.AUDIT.name to "false",
                LogTypes.DEBUG.name to "true",
                LogTypes.TRACE.name to "false")
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals("expected keys: [AUDIT, DEBUG, WARN, TRACE]. received keys: [AUDIT, DEBUG, TRACE]", ex.message)
    }

    @Test
    fun testShouldComplain_missingDebug() {
        val data = mapOf(
                LogTypes.AUDIT.name to "false",
                LogTypes.WARN.name to "false",
                LogTypes.TRACE.name to "false")
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals("expected keys: [AUDIT, DEBUG, WARN, TRACE]. received keys: [AUDIT, WARN, TRACE]", ex.message)
    }

    @Test
    fun testShouldComplain_missingTrace() {
        val data = mapOf(
                LogTypes.AUDIT.name to "false",
                LogTypes.WARN.name to "false",
                LogTypes.DEBUG.name to "false")
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals("expected keys: [AUDIT, DEBUG, WARN, TRACE]. received keys: [AUDIT, WARN, DEBUG]", ex.message)
    }



    /**
     * If the user somehow sets the inputs to something other than "true" or "false", complain
     */
    @Test
    fun testShouldComplainAboutBadInput_audit() {
        val data = allTrue(audit = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_warn() {
        val data = allTrue(warn = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_debug() {
        val data = allTrue(debug = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_trace() {
        val data = allTrue(trace = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun allFalse(audit: String = "false",
                         warn: String = "false",
                         debug: String = "false",
                         trace: String = "false"): Map<String, String> {
        return mapOf(
                LogTypes.AUDIT.name to audit,
                LogTypes.WARN.name to warn,
                LogTypes.DEBUG.name to debug,
                LogTypes.TRACE.name to trace)
    }

    private fun allTrue(audit: String = "true",
                        warn: String = "true",
                        debug: String = "true",
                        trace: String = "true"): Map<String, String> {
        return mapOf(
                LogTypes.AUDIT.name to audit,
                LogTypes.WARN.name to warn,
                LogTypes.DEBUG.name to debug,
                LogTypes.TRACE.name to trace)
    }

}