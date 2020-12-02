package coverosR3z.misc

import coverosR3z.domainobjects.NO_USER
import coverosR3z.domainobjects.SYSTEM_USER
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import coverosR3z.logging.resetLogSettingsToDefault
import coverosR3z.server.PreparedResponseData
import coverosR3z.server.ResponseStatus
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
    fun testShouldGetLoggingPageIfAuthenticated() {
        val result : PreparedResponseData = handleGETLogging(SYSTEM_USER)
        assertEquals(ResponseStatus.OK, result.responseStatus)
    }

    /**
     * Make sure it handles properly if we are authenticated
     */
    @Test
    fun testShouldPostLoggingPageIfAuthenticated() {
        val data = mapOf("info" to "true", "warn" to "false", "debug" to "false", "trace" to "false")
        val result : PreparedResponseData = handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(ResponseStatus.OK, result.responseStatus)
    }

    /**
     * Should redirect to the homepage if unauthenticated
     */
    @Test
    fun testShouldNotGetLoggingPageIfUnauthenticated() {
        val result : PreparedResponseData = handleGETLogging(NO_USER)
        assertEquals(ResponseStatus.SEE_OTHER, result.responseStatus)
    }

    /**
     * Should redirect to the homepage if unauthenticated
     */
    @Test
    fun testShouldNotPostLoggingPageIfUnauthenticated() {
        val result : PreparedResponseData = handlePOSTLogging(NO_USER, emptyMap())
        assertEquals(ResponseStatus.SEE_OTHER, result.responseStatus)
    }

    /**
     * If we
     */
    @Test
    fun testShouldChangeConfiguration_setInfoTrueOnly() {
        val data = mapOf("info" to "true", "warn" to "false", "debug" to "false", "trace" to "false")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(true, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setWarnTrueOnly() {
        val data = mapOf("info" to "false", "warn" to "true", "debug" to "false", "trace" to "false")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(true, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setDebugTrueOnly() {
        val data = mapOf("info" to "false", "warn" to "false", "debug" to "true", "trace" to "false")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(true, logSettings[LogTypes.DEBUG])
        assertEquals(false, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_setTraceTrueOnly() {
        val data = mapOf("info" to "false", "warn" to "false", "debug" to "false", "trace" to "true")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(false, logSettings[LogTypes.AUDIT])
        assertEquals(false, logSettings[LogTypes.WARN])
        assertEquals(false, logSettings[LogTypes.DEBUG])
        assertEquals(true, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_allOn() {
        val data = mapOf("info" to "true", "warn" to "true", "debug" to "true", "trace" to "true")
        handlePOSTLogging(SYSTEM_USER, data)
        assertEquals(true, logSettings[LogTypes.AUDIT])
        assertEquals(true, logSettings[LogTypes.WARN])
        assertEquals(true, logSettings[LogTypes.DEBUG])
        assertEquals(true, logSettings[LogTypes.TRACE])
    }

    @Test
    fun testShouldChangeConfiguration_allOff() {
        val data = mapOf("info" to "false", "warn" to "false", "debug" to "false", "trace" to "false")
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
    fun testShouldComplain_missingInfo() {
        val data = mapOf("warn" to "false", "debug" to "false", "trace" to "false")
        val ex = assertThrows(IllegalStateException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(missingLoggingDataInputMsg, ex.message)
    }

    @Test
    fun testShouldComplain_missingWarn() {
        val data = mapOf("info" to "false", "debug" to "false", "trace" to "false")
        val ex = assertThrows(IllegalStateException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(missingLoggingDataInputMsg, ex.message)
    }

    @Test
    fun testShouldComplain_missingDebug() {
        val data = mapOf("info" to "false", "warn" to "false", "trace" to "false")
        val ex = assertThrows(IllegalStateException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(missingLoggingDataInputMsg, ex.message)
    }

    @Test
    fun testShouldComplain_missingTrace() {
        val data = mapOf("info" to "false", "warn" to "false", "debug" to "false")
        val ex = assertThrows(IllegalStateException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(missingLoggingDataInputMsg, ex.message)
    }

    /**
     * If the user somehow sets the inputs to something other than "true" or "false", complain
     */
    @Test
    fun testShouldComplainAboutBadInput_info() {
        val data = mapOf("info" to "foo", "warn" to "false", "debug" to "false", "trace" to "false")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_warn() {
        val data = mapOf("info" to "false", "warn" to "foo", "debug" to "false", "trace" to "false")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_debug() {
        val data = mapOf("info" to "false", "warn" to "false", "debug" to "foo", "trace" to "false")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }

    @Test
    fun testShouldComplainAboutBadInput_trace() {
        val data = mapOf("info" to "false", "warn" to "false", "debug" to "false", "trace" to "foo")
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogging(SYSTEM_USER, data)}
        assertEquals(badInputLoggingDataMsg, ex.message)
    }


}