package coverosR3z.timerecording

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.fakeTechempower
import coverosR3z.misc.DEFAULT_EMPLOYEE_USER
import coverosR3z.misc.DEFAULT_USER
import coverosR3z.misc.testLogger
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.SubmitTimeAPI
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.TimePeriod
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class SubmitTimeAPITests {

    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        val pmd = PureMemoryDatabase()
        val cu = CurrentUser(DEFAULT_USER)
        au = AuthenticationUtilities(
            AuthenticationPersistence(pmd, logger = testLogger),
            testLogger,
            CurrentUser(DEFAULT_EMPLOYEE_USER)
        )
        tru = TimeRecordingUtilities(TimeEntryPersistence(pmd, logger = testLogger), cu, testLogger)
    }


    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime() {
        val startDate = "2021-01-01"
        val endDate = "2021-01-15"
        val data = PostBodyData(mapOf(
            SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
            SubmitTimeAPI.Elements.END_DATE.getElemName() to endDate,
        ))
        val sd = makeServerData(data)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode

        val timePeriod = TimePeriod(Date.make(startDate), Date.make(endDate))

        // get the state from the database so we can confirm the time period was persisted
        val stp = tru.getSubmittedTimePeriod(timePeriod)
        assertEquals(stp.bounds, timePeriod)

        // confirm we return the user to the right place
        assertEquals(
            "We should have gotten redirected to the viewTime page",
            StatusCode.SEE_OTHER, response
        )
    }

    private fun makeServerData(data: PostBodyData): ServerData {
        val sd = ServerData(
            BusinessCode(tru, au, fakeTechempower),
            fakeServerObjects,
            AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger
        )
        return sd
    }
}