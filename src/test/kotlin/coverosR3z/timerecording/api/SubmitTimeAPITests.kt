package coverosR3z.timerecording.api

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.*
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.ApprovalStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalStateException

class SubmitTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities
    private val defaultStartDate = "2021-01-01"

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // region role tests

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_RegularUser() {
        val sd = makeSdForSubmit(DEFAULT_REGULAR_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_ApproverUser() {
        val sd = makeSdForSubmit(DEFAULT_APPROVER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_AdminUser() {
        val sd = makeSdForSubmit(DEFAULT_ADMIN_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_SystemUser() {
        val sd = makeSdForSubmit(SYSTEM_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.FORBIDDEN, response)
    }

    // endregion

    @Test
    fun testSubmittingTime_InvalidStartDate() {
        val sd = makeSdForSubmit(startDate = "a1")

        // the API processes the client input
        val ex = assertThrows(IllegalStateException::class.java) { SubmitTimeAPI.handlePost(sd) }
        assertEquals("""The date for submitting time was not interpreted as a date. You sent "a1".  Format is YYYY-MM-DD""", ex.message)
    }

    /**
     * If you pass in [coverosR3z.timerecording.api.SubmitTimeAPI.Elements.UNSUBMIT] set to "true",
     * it will unsubmit your time
     */
    @Test
    fun testUnsubmittingTime() {
        val sd = makeSdForSubmit(unsubmit = true)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    /**
     * If you try to unsubmit time that is already approved,
     * it will fail
     */
    @Test
    fun testUnsubmittingApprovedTime() {
        tru.isApprovedBehavior = { ApprovalStatus.APPROVED }
        val sd = makeSdForSubmit(unsubmit = true)

        // the API processes the client input
        val ex = assertThrows(IllegalStateException::class.java) { SubmitTimeAPI.handlePost(sd).statusCode }
        assertEquals("This time period is approved.  Cannot operate on approved time periods.", ex.message)
    }

    /**
     * If you try to submit time that is already submitted,
     * it will fail
     */
    @Test
    fun testSubmittingAlreadySubmitted() {
        tru.isInASubmittedPeriodBehavior = { true }
        val sd = makeSdForSubmit()

        // the API processes the client input
        val ex = assertThrows(IllegalStateException::class.java) { SubmitTimeAPI.handlePost(sd).statusCode }
        assertEquals("This time period is already submitted.  Cannot submit on this period again.", ex.message)
    }

    /**
     * A test helper for this class, just to remove repetitive boilerplate
     */
    private fun makeSdForSubmit(user: User = DEFAULT_REGULAR_USER, startDate: String = defaultStartDate, unsubmit: Boolean = false): ServerData {
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
                SubmitTimeAPI.Elements.UNSUBMIT.getElemName() to unsubmit.toString(),
            )
        )
        return makeServerData(data, tru, au, user = user)
    }

}