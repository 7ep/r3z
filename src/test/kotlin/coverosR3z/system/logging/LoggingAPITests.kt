package coverosR3z.system.logging

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.server.APITestCategory
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.api.CreateEmployeeAPI
import org.junit.After
import org.junit.Assert
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
        val sd = makeLoggingServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(true,  testLogger.logSettings.audit)
        assertEquals(false, testLogger.logSettings.warn)
        assertEquals(false, testLogger.logSettings.debug)
        assertEquals(false, testLogger.logSettings.trace)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setWarnTrueOnly() {
        val data = allFalse(warn = "true")
        val sd = makeLoggingServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings.audit)
        assertEquals(true,  testLogger.logSettings.warn)
        assertEquals(false, testLogger.logSettings.debug)
        assertEquals(false, testLogger.logSettings.trace)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setDebugTrueOnly() {
        val data = allFalse(debug = "true")
        val sd = makeLoggingServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings.audit)
        assertEquals(false, testLogger.logSettings.warn)
        assertEquals(true,  testLogger.logSettings.debug)
        assertEquals(false, testLogger.logSettings.trace)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_setTraceTrueOnly() {
        val data = allFalse(trace = "true")
        val sd = makeLoggingServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings.audit)
        assertEquals(false, testLogger.logSettings.warn)
        assertEquals(false, testLogger.logSettings.debug)
        assertEquals(true,  testLogger.logSettings.trace)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_allOn() {
        val data = allTrue()
        val sd = makeLoggingServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(true, testLogger.logSettings.audit)
        assertEquals(true, testLogger.logSettings.warn)
        assertEquals(true, testLogger.logSettings.debug)
        assertEquals(true, testLogger.logSettings.trace)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldChangeConfiguration_allOff() {
        val data = allFalse()
        val sd = makeLoggingServerData(data)

        LoggingAPI.handlePost(sd)

        assertEquals(false, testLogger.logSettings.audit)
        assertEquals(false, testLogger.logSettings.warn)
        assertEquals(false, testLogger.logSettings.debug)
        assertEquals(false, testLogger.logSettings.trace)
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
        val sd = makeLoggingServerData(data)

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
        val sd = makeLoggingServerData(data)

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

        val sd = makeLoggingServerData(data)

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
        val sd = makeLoggingServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){  LoggingAPI.handlePost(sd) }

        assertEquals("expected keys: [audit, warn, debug, trace]. received keys: [audit, warn, debug]", ex.message)
    }



    /**
     * If the user somehow sets the inputs to something other than "true" or "false", complain
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_audit() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "input for log setting must be \"true\" or \"false\"",
            false,
            LoggingAPI.path)
        val data = allTrue(audit = "foo")
        val sd = makeLoggingServerData(data)

        val result = LoggingAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_warn() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "input for log setting must be \"true\" or \"false\"",
            false,
            LoggingAPI.path)
        val data = allTrue(warn = "foo")
        val sd = makeLoggingServerData(data)

        val result = LoggingAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_debug() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "input for log setting must be \"true\" or \"false\"",
            false,
            LoggingAPI.path)
        val data = allTrue(debug = "foo")
        val sd = makeLoggingServerData(data)

        val result = LoggingAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldComplainAboutBadInput_trace() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "input for log setting must be \"true\" or \"false\"",
            false,
            LoggingAPI.path)
        val data = allTrue(trace = "foo")
        val sd = makeLoggingServerData(data)

        val result = LoggingAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    // region ROLES TESTS

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminDoPost() {
        val sd = makeLoggingServerData(allTrue())
        val result = LoggingAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemDoPost() {
        val sd = makeLoggingServerData(allTrue(), user = SYSTEM_USER)

        val result = LoggingAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowApproverDoPost() {
        val sd = makeLoggingServerData(allTrue(), user = DEFAULT_APPROVER_USER)

        val result = LoggingAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowRegularUserDoPost() {
        val sd = makeLoggingServerData(allTrue(), user = DEFAULT_REGULAR_USER)

        val result = LoggingAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminDoGet() {
        val sd = makeLoggingServerData(allTrue())
        val result = LoggingAPI.handleGet(sd).statusCode
        assertEquals(StatusCode.OK, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemDoGet() {
        val sd = makeLoggingServerData(allTrue(), user = SYSTEM_USER)

        val result = LoggingAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowApproverDoGet() {
        val sd = makeLoggingServerData(allTrue(), user = DEFAULT_APPROVER_USER)

        val result = LoggingAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowRegularUserDoGet() {
        val sd = makeLoggingServerData(allTrue(), user = DEFAULT_REGULAR_USER)

        val result = LoggingAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion
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

    private fun makeLoggingServerData(data: PostBodyData, user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = LoggingAPI.path)
    }
}