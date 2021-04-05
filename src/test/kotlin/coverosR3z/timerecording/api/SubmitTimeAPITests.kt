package coverosR3z.timerecording.api

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.*
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class SubmitTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // region role tests

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_RegularUser() {
        val startDate = "2021-01-01"
        val endDate = "2021-01-15"
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
                SubmitTimeAPI.Elements.END_DATE.getElemName() to endDate,
            )
        )
        val sd = makeServerData(data, tru, au, user = DEFAULT_REGULAR_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_ApproverUser() {
        val startDate = "2021-01-01"
        val endDate = "2021-01-15"
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
                SubmitTimeAPI.Elements.END_DATE.getElemName() to endDate,
            )
        )
        val sd = makeServerData(data, tru, au, user = DEFAULT_APPROVER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_AdminUser() {
        val startDate = "2021-01-01"
        val endDate = "2021-01-15"
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
                SubmitTimeAPI.Elements.END_DATE.getElemName() to endDate,
            )
        )
        val sd = makeServerData(data, tru, au, user = DEFAULT_ADMIN_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_SystemUser() {
        val startDate = "2021-01-01"
        val endDate = "2021-01-15"
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
                SubmitTimeAPI.Elements.END_DATE.getElemName() to endDate,
            )
        )
        val sd = makeServerData(data, tru, au, user = SYSTEM_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.FORBIDDEN, response)
    }

    // end region

}