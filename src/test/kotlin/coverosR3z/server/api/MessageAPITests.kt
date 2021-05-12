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
    fun testMessageAPI_ShouldShowMessage() {
        val sd = makeMessageServerData()

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("The log settings were saved"))
    }

    /**
     * There should be a link to wherever we need to send the user
     */
    @Test
    fun testMessageAPI_ShouldIncludeLinkToProperPage() {
        val sd = makeMessageServerData()

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""href="${LoggingAPI.path}""""))
    }

    /**
     * The message we display could be something indicating success or failure.
     * See [MessageAPI.MessageType]
     */
    @Test
    fun testMessageAPI_ShouldIncludeTypeOfMessage() {
        val sd = makeMessageServerData()

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""class="success""""))
    }

    /**
     * If neither the [MessageAPI.enumeratedMessageKey] nor the [MessageAPI.customMessageKey] is sent,
     * show "NO MESSAGE"
     */
    @Test
    fun testMessageAPI_InvalidMessageCode_noMessagesSent() {
        val sd = makeMessageServerData(queryString = emptyMap())

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""NO MESSAGE"""))
    }

    /**
     * If the key for an enumerated message is sent empty, show NO MATCHING MESSAGE
     */
    @Test
    fun testMessageAPI_InvalidMessageCode_emptyEnumValueSent() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.enumeratedMessageKey to "",
        ))

        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""NO MATCHING MESSAGE"""))
    }

    /**
     * If the message code doesn't match a known message enum and
     * no custom message was sent, show "NO MATCHING MESSAGE"
     */
    @Test
    fun testMessageAPI_InvalidMessageCode_NoMatch() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.enumeratedMessageKey to "NO MATCH",
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""NO MATCHING MESSAGE"""))
    }

    /**
     * We can also use this API to send custom messages as part of the
     * query string.  In that case, you send along the path for it to
     * return to, and a message to be shown.  For example, the page
     * might end up showing:
     *
     * Hi, this is the message
     *
     * _return_
     *
     */
    @Test
    fun testMessageAPI_CustomMessageAndReturn() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "This is the message we show",
            MessageAPI.customReturnLinkKey to "some_path",
            MessageAPI.customIsSuccessKey to "true"
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""This is the message we show"""))
        assertTrue(result.contains("""href="some_path""""))
        assertTrue(result.contains("""class="${MessageAPI.MessageType.SUCCESS.toString().toLowerCase()}""""))
    }

    /**
     * We can indicate whether the message is classed as an indicator of success or not.
     * Generally, we indicate it isn't if we are showing that something failed.
     */
    @Test
    fun testMessageAPI_CustomMessageAndReturn_unsuccessful() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "This is the message we show",
            MessageAPI.customReturnLinkKey to "some_path",
            MessageAPI.customIsSuccessKey to "false"
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""This is the message we show"""))
        assertTrue(result.contains("""href="some_path""""))
        assertTrue(result.contains("""class="${MessageAPI.MessageType.FAILURE.toString().toLowerCase()}""""))
    }

    /**
     * Similar to [testMessageAPI_CustomMessageAndReturn_unsuccessful] but we pass
     * in something to [MessageAPI.customIsSuccessKey] that is neither "true" nor "false",
     * so it should become false
     */
    @Test
    fun testMessageAPI_CustomMessageAndReturn_unsuccessful_FOOIsFalse() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "This is the message we show",
            MessageAPI.customReturnLinkKey to "some_path",
            MessageAPI.customIsSuccessKey to "FOO"
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""This is the message we show"""))
        assertTrue(result.contains("""href="some_path""""))
        assertTrue(result.contains("""class="${MessageAPI.MessageType.FAILURE.toString().toLowerCase()}""""))
    }

    /**
     * We don't currently allow passing both custom messages and enumerated messages
     */
    @Test
    fun testMessageAPI_CustomAndEnum() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "This is the message we show",
            MessageAPI.customReturnLinkKey to "some_path",
            MessageAPI.enumeratedMessageKey to MessageAPI.Message.LOG_SETTINGS_SAVED.toString(),
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""MIXED MESSAGE TYPES"""))
    }

    /**
     * If we don't sent the key for [MessageAPI.customIsSuccessKey],
     * it defaults to false = failure
     */
    @Test
    fun testMessageAPI_CustomMissingIsSuccess() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "This is the message we show",
            MessageAPI.customReturnLinkKey to "some_path",
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""class="failure""""))
    }

    /**
     * If we don't send a return link, it defaults to homepage
     */
    @Test
    fun testMessageAPI_CustomMissingReturnLink() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "This is the message we show",
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains(""">This is the message we show<"""))
        assertTrue(result.contains("""href="homepage""""))
    }

    /**
     * If the custom message is empty,
     * it shows NO MESSAGE
     */
    @Test
    fun testMessageAPI_CustomEmptyMessage() {
        val sd = makeMessageServerData(queryString = mapOf(
            MessageAPI.customMessageKey to "",
        ))
        val result = MessageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""NO MESSAGE"""))
        assertTrue(result.contains("""href="homepage""""))
    }

    /**
     * Creats a [ServerData] with a default user of [DEFAULT_ADMIN_USER] and
     * a default query string with a message code of "1"
     */
    private fun makeMessageServerData(
        user: User = DEFAULT_ADMIN_USER,
        queryString: Map<String, String> = mapOf(
            MessageAPI.enumeratedMessageKey to MessageAPI.Message.LOG_SETTINGS_SAVED.name,
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