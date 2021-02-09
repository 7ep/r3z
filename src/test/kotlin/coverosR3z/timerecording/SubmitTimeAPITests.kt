package coverosR3z.timerecording

import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.SubmitTimeAPI
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SubmitTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    @Test
    fun testSubmittingTime() {
        val data = PostBodyData(mapOf(
            SubmitTimeAPI.Elements.START_DATE.getElemName() to "2021-01-01",
            SubmitTimeAPI.Elements.END_DATE.getElemName() to "2021-01-15",
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        Assert.assertEquals(
            "We should have gotten redirected to the viewTime page",
            StatusCode.SEE_OTHER, response
        )
    }
}