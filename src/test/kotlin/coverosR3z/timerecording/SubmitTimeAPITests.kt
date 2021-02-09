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


    /** Testing submission:
     * "submit" button (theoretically) will trigger (submit current time period)
     * this will look up time period that matches current date. Identify start/end, probably from some function (might also search database for existing time periods)
     * - API level function (submit for "some" date)
     * - unit level function that does the mechanics
     */


    /**
     * submit today's time period.
     * input: none (user is known by session)
     * result: determine current time period]based on today's date and submit all entries associated with it
     */
    @Test
    fun testSubmittingTime_todaysDate() {
        val data = PostBodyData(mapOf(
            2021-01-01",
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        // handle the post which will trigger the 'submitTimePeriod' capability which needs a date within time period
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        Assert.assertEquals(
            "We should have gotten redirected to the viewTime page",
            StatusCode.SEE_OTHER, response
        )
    }
}