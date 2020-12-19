package coverosR3z.logging

import coverosR3z.exceptions.InexactInputsException
import coverosR3z.logging.LogConfig.logSettings
import coverosR3z.server.AuthStatus
import coverosR3z.server.doPOSTAuthenticated
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
     * If we
     */
    @Test
    fun testShouldChangeConfiguration_setAuditTrueOnly() {
        val data = allFalse(audit = "true")
        doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }
        assertEquals(true, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setWarnTrueOnly() {
        val data = allFalse(warn = "true")
        doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(true, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setDebugTrueOnly() {
        val data = allFalse(debug = "true")
        doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(true, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setTraceTrueOnly() {
        val data = allFalse(trace = "true")
        doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(true, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_allOn() {
        val data = allTrue()
        doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }
        assertEquals(true, logSettings[LogTypes.AUDIT])
        assertEquals(true, logSettings[LogTypes.WARN])
        assertEquals(true, logSettings[LogTypes.DEBUG])
        assertEquals(true, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_allOff() {
        val data = allFalse()
        doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }
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
                LoggingAPI.Elements.WARN_INPUT.elemName to "false",
                LoggingAPI.Elements.DEBUG_INPUT.elemName to "true",
                LoggingAPI.Elements.TRACE_INPUT.elemName to "false")
        val ex = assertThrows(InexactInputsException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [warn, debug, trace]", ex.message)
    }

    @Test
    fun testShouldComplain_missingWarn() {
        val data = mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.elemName to "false",
                LoggingAPI.Elements.DEBUG_INPUT.elemName to "true",
                LoggingAPI.Elements.TRACE_INPUT.elemName to "false")
        val ex = assertThrows(InexactInputsException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, debug, trace]", ex.message)
    }

    @Test
    fun testShouldComplain_missingDebug() {
        val data = mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.elemName to "false",
                LoggingAPI.Elements.WARN_INPUT.elemName to "false",
                LoggingAPI.Elements.TRACE_INPUT.elemName to "false")
        val ex = assertThrows(InexactInputsException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, warn, trace]", ex.message)
    }

    @Test
    fun testShouldComplain_missingTrace() {
        val data = mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.elemName to "false",
                LoggingAPI.Elements.WARN_INPUT.elemName to "false",
                LoggingAPI.Elements.DEBUG_INPUT.elemName to "false")
        val ex = assertThrows(InexactInputsException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, warn, debug]", ex.message)
    }



    /**
     * If the user somehow sets the inputs to something other than "true" or "false", complain
     */
    @Test
    fun testShouldComplainAboutBadInput_audit() {
        val data = allTrue(audit = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_warn() {
        val data = allTrue(warn = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_debug() {
        val data = allTrue(debug = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_trace() {
        val data = allTrue(trace = "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){doPOSTAuthenticated(AuthStatus.AUTHENTICATED, LoggingAPI.requiredInputs, data) { LoggingAPI.handlePOST(data) }}
        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
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
                LoggingAPI.Elements.AUDIT_INPUT.elemName to audit,
                LoggingAPI.Elements.WARN_INPUT.elemName to warn,
                LoggingAPI.Elements.DEBUG_INPUT.elemName to debug,
                LoggingAPI.Elements.TRACE_INPUT.elemName to trace)
    }

    private fun allTrue(audit: String = "true",
                        warn: String = "true",
                        debug: String = "true",
                        trace: String = "true"): Map<String, String> {
        return mapOf(
            LoggingAPI.Elements.AUDIT_INPUT.elemName to audit,
            LoggingAPI.Elements.WARN_INPUT.elemName to warn,
            LoggingAPI.Elements.DEBUG_INPUT.elemName to debug,
            LoggingAPI.Elements.TRACE_INPUT.elemName to trace)
    }

}