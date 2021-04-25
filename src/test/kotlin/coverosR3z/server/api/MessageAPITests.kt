package coverosR3z.server.api

import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.system.logging.LoggingAPI
import coverosR3z.system.misc.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalStateException

@Category(APITestCategory::class)
class MessageAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A message should be displayed to the user
     */
    @Test
    fun testShouldShowMessage() {
        val sd = makePostResultSd()

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("The log settings were saved"))
    }

    /**
     * There should be a link to wherever we need to send the user
     */
    @Test
    fun testShouldIncludeLinkToProperPage() {
        val sd = makePostResultSd()

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""href="${LoggingAPI.path}""""))
    }

    /**
     * The message we display could be something indicating success or failure.
     * See [MessageAPI.MessageType]
     */
    @Test
    fun testShouldIncludeTypeOfMessage() {
        val sd = makePostResultSd()

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""class="success""""))
    }

    /**
     * If the message code isn't sent, it's null
     */
    @Test
    fun testInvalidMessageCode_Null() {
        val sd = makePostResultSd(queryString = emptyMap())
        val ex = assertThrows(IllegalStateException::class.java) { MessageAPI.handleGet(sd).fileContentsString() }
        assertEquals("This requires a message code - was null", ex.message)
    }

    /**
     * If the message code doesn't match a known message enum
     */
    @Test
    fun testInvalidMessageCode_NoMatch() {
        val sd = makePostResultSd(queryString = mapOf(
            MessageAPI.queryStringKey to "NO MATCH",
        ))
        val ex = assertThrows(IllegalStateException::class.java) { MessageAPI.handleGet(sd).fileContentsString() }
        assertEquals("No matching message code was provided", ex.message)
    }

    /**
     * Creats a [ServerData] with a default user of [DEFAULT_ADMIN_USER] and
     * a default query string with a message code of "1"
     */
    private fun makePostResultSd(
        user: User = DEFAULT_ADMIN_USER,
        queryString: Map<String, String> = mapOf(
            MessageAPI.queryStringKey to MessageAPI.Message.LOG_SETTINGS_SAVED.name,
        ),
        authStatus: AuthStatus = AuthStatus.AUTHENTICATED
    ): ServerData {
        return ServerData(
            BusinessCode(tru, au),
            fakeServerObjects,
            AnalyzedHttpData(data = PostBodyData(), user = user, queryString = queryString),
            authStatus = authStatus,
            testLogger
        )
    }
}