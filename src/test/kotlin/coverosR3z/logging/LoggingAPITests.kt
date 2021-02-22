package coverosR3z.logging

import coverosR3z.misc.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.fakeTechempower
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.testLogger
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class LoggingAPITests {

    @Before
    fun init() {
        testLogger.resetLogSettingsToDefault()
    }

    @After
    fun cleanup() {
        testLogger.resetLogSettingsToDefault()
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setAuditTrueOnly() {
        val data = allFalse(audit = "true")
        val sd = makeServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(true,  testLogger.logSettings[LogTypes.AUDIT])
        assertEquals(false, testLogger.logSettings[LogTypes.WARN])
        assertEquals(false, testLogger.logSettings[LogTypes.DEBUG])
        assertEquals(false, testLogger.logSettings[LogTypes.TRACE])
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setWarnTrueOnly() {
        val data = allFalse(warn = "true")
        val sd = makeServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings[LogTypes.AUDIT])
        assertEquals(true,  testLogger.logSettings[LogTypes.WARN])
        assertEquals(false, testLogger.logSettings[LogTypes.DEBUG])
        assertEquals(false, testLogger.logSettings[LogTypes.TRACE])
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setDebugTrueOnly() {
        val data = allFalse(debug = "true")
        val sd = makeServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings[LogTypes.AUDIT])
        assertEquals(false, testLogger.logSettings[LogTypes.WARN])
        assertEquals(true,  testLogger.logSettings[LogTypes.DEBUG])
        assertEquals(false, testLogger.logSettings[LogTypes.TRACE])
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setTraceTrueOnly() {
        val data = allFalse(trace = "true")
        val sd = makeServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings[LogTypes.AUDIT])
        assertEquals(false, testLogger.logSettings[LogTypes.WARN])
        assertEquals(false, testLogger.logSettings[LogTypes.DEBUG])
        assertEquals(true,  testLogger.logSettings[LogTypes.TRACE])
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_allOn() {
        val data = allTrue()
        val sd = makeServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(true, testLogger.logSettings[LogTypes.AUDIT])
        assertEquals(true, testLogger.logSettings[LogTypes.WARN])
        assertEquals(true, testLogger.logSettings[LogTypes.DEBUG])
        assertEquals(true, testLogger.logSettings[LogTypes.TRACE])
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_allOff() {
        val data = allFalse()
        val sd = makeServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings[LogTypes.AUDIT])
        assertEquals(false, testLogger.logSettings[LogTypes.WARN])
        assertEquals(false, testLogger.logSettings[LogTypes.DEBUG])
        assertEquals(false, testLogger.logSettings[LogTypes.TRACE])
    }

    /**
     * We require four inputs from the user.  If any are missing, complain
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldComplain_missingAudit() {
        val data = PostBodyData(mapOf(
                LoggingAPI.Elements.WARN_INPUT.getElemName() to "false",
                LoggingAPI.Elements.DEBUG_INPUT.getElemName() to "true",
                LoggingAPI.Elements.TRACE_INPUT.getElemName() to "false"))
        val sd = makeServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [warn, debug, trace]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplain_missingWarn() {
        val data = PostBodyData(mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.getElemName() to "false",
                LoggingAPI.Elements.DEBUG_INPUT.getElemName() to "true",
                LoggingAPI.Elements.TRACE_INPUT.getElemName() to "false"))
        val sd = makeServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, debug, trace]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplain_missingDebug() {
        val data = PostBodyData(mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.getElemName() to "false",
                LoggingAPI.Elements.WARN_INPUT.getElemName() to "false",
                LoggingAPI.Elements.TRACE_INPUT.getElemName() to "false"))

        val sd = makeServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, warn, trace]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplain_missingTrace() {
        val data = PostBodyData(mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.getElemName() to "false",
                LoggingAPI.Elements.WARN_INPUT.getElemName() to "false",
                LoggingAPI.Elements.DEBUG_INPUT.getElemName() to "false"))
        val sd = makeServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, warn, debug]", ex.message)
    }



    /**
     * If the user somehow sets the inputs to something other than "true" or "false", complain
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_audit() {
        val data = allTrue(audit = "foo")
        val sd = makeServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_warn() {
        val data = allTrue(warn = "foo")
        val sd = makeServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_debug() {
        val data = allTrue(debug = "foo")
        val sd = makeServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_trace() {
        val data = allTrue(trace = "foo")
        val sd = makeServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals(LoggingAPI.badInputLoggingDataMsg, ex.message)
    }

    private fun makeServerData(data: PostBodyData): ServerData {
        val sd = ServerData(
            BusinessCode(tru, au, fakeTechempower),
            fakeServerObjects,
            AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.AUTHENTICATED, testLogger
        )
        return sd
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
                         trace: String = "false"): PostBodyData {
        return PostBodyData(mapOf(
                LoggingAPI.Elements.AUDIT_INPUT.getElemName() to audit,
                LoggingAPI.Elements.WARN_INPUT.getElemName() to warn,
                LoggingAPI.Elements.DEBUG_INPUT.getElemName() to debug,
                LoggingAPI.Elements.TRACE_INPUT.getElemName() to trace))
    }

    private fun allTrue(audit: String = "true",
                        warn: String = "true",
                        debug: String = "true",
                        trace: String = "true"): PostBodyData {
        return PostBodyData(mapOf(
            LoggingAPI.Elements.AUDIT_INPUT.getElemName() to audit,
            LoggingAPI.Elements.WARN_INPUT.getElemName() to warn,
            LoggingAPI.Elements.DEBUG_INPUT.getElemName() to debug,
            LoggingAPI.Elements.TRACE_INPUT.getElemName() to trace))
    }

    companion object {
        val tru = FakeTimeRecordingUtilities()
        val au = FakeAuthenticationUtilities()
    }

}