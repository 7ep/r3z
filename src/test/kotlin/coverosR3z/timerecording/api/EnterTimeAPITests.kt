package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.server.APITestCategory
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.NO_PROJECT
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class EnterTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
        tru.findProjectByNameBehavior = { DEFAULT_PROJECT }
    }

    /**
     * If we pass in valid information, it should indicate success
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI() {
        val result = enterTimeWithAPI()

        assertSuccessfulTimeEntry(result)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingProject() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }

        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [time_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingTimeEntry() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }

        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingDetailEntry() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }

        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingDateEntry() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }

        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, detail_entry]", ex.message)
    }

    /**
     * If we pass in an empty string for project
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_emptyStringProject() {
        val expected =
            MessageAPI.createCustomMessageRedirect("Makes no sense to have an empty project name", false, ViewTimeAPI.path)

        val result = enterTimeWithAPI(proj = "")

        assertEquals(expected, result)
    }

    /**
     * If we pass in all spaces as the project
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_allSpacesProject() {
        val expected =
            MessageAPI.createCustomMessageRedirect("Makes no sense to have an empty project name", false, ViewTimeAPI.path)

        val result = enterTimeWithAPI(proj = "   ")

        assertEquals(expected, result)
    }

    /**
     * If the project passed in isn't recognized
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_unrecognizedProject() {
        tru.findProjectByNameBehavior = { NO_PROJECT }
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.INVALID_PROJECT_DURING_ENTERING_TIME)

        val result = enterTimeWithAPI(proj = "UNRECOGNIZED")

        assertEquals(expected, result)
    }

    /**
     * If the time entered is negative
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_negativeTime() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "Doesn't make sense to have negative time. time in minutes: -60",
            false,
            ViewTimeAPI.path
        )

        val result = enterTimeWithAPI(time = "-1")

        assertEquals(expected, result)
    }

    /**
     * You can only enter 24 hours in a day
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_greaterThanTwentyFour() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.TIME_MUST_BE_LESS_OR_EQUAL_TO_24)

        val result = enterTimeWithAPI(time = "24.50")

        assertEquals(expected, result)
    }

    /**
     * If the time entered is zero, it's fine.
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_zeroTime() {
        val result = enterTimeWithAPI(time = "0")

        assertSuccessfulTimeEntry(result)
    }

    /**
     * If the time entered is non-numeric, like "a"
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_nonNumericTime() {
        val expected =
            MessageAPI.createCustomMessageRedirect("Must be able to parse \"a\" as a double", false, ViewTimeAPI.path)

        val result = enterTimeWithAPI(time = "a")

        assertEquals(expected, result)
    }

    /**
     * The time must be a valid multiple of 0.50
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_timeNotOnValidMultiple() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.MINUTES_MUST_BE_MULTIPLE_OF_HALF_HOUR)

        val result = enterTimeWithAPI(time = "1.49")

        assertEquals(expected, result)
    }

    /**
     * If a time period (future or past, doesn't matter) has been submitted, it isn't possible to
     * create a new time entry for it.
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTime_DateInvalid_DateEntryDisallowedForSubmittedTime() {
        tru.isInASubmittedPeriodBehavior = { true }
        val expected =
            MessageAPI.createEnumMessageRedirect(MessageAPI.Message.NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD)

        val result = enterTimeWithAPI()

        assertEquals(expected, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_EmptyDateString() {
        val expected = MessageAPI.createCustomMessageRedirect("date must not be blank", false, ViewTimeAPI.path)

        val result = enterTimeWithAPI(date = "")

        assertEquals(expected, result)
    }

    /**
     * If the date is before [coverosR3z.system.misc.types.earliestAllowableDate]
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_TooEarlyDateString() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "no way on earth people are using this before 1980-01-01 or past 2200-01-01, you had a date of 1979-12-31",
            false,
            ViewTimeAPI.path
        )

        val result = enterTimeWithAPI(date = "1979-12-31")

        assertEquals(expected, result)
    }

    /**
     * If the date is after [coverosR3z.system.misc.types.latestAllowableDate]
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_TooLateDateString() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "no way on earth people are using this before 1980-01-01 or past 2200-01-01, you had a date of 2200-01-02",
            false,
            ViewTimeAPI.path
        )

        val result = enterTimeWithAPI(date = "2200-01-02")

        assertEquals(expected, result)
    }

    // region ROLE TESTS

    // POST tests


    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = DEFAULT_ADMIN_USER)

        val result = EnterTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowApproverToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = DEFAULT_APPROVER_USER)

        val result = EnterTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowRegularToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = DEFAULT_REGULAR_USER)

        val result = EnterTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = SYSTEM_USER)

        val result = EnterTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowNoUserToEnterTimePOST() {
        val sd = makeETServerData(PostBodyData(), user = NO_USER)

        assertEquals(StatusCode.UNAUTHORIZED, EnterTimeAPI.handlePost(sd).statusCode)
    }

    @Test
    fun testEdit() {
        tru.findEmployeeByIdBehavior = { DEFAULT_REGULAR_USER.employee }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.BEING_EDITED.getElemName() to "true",
        ))
        val sd = makeETServerData(data)

        val response = EnterTimeAPI.handlePost(sd).statusCode

        assertEquals(
            "We should have gotten redirected to the viewTime page",
            StatusCode.SEE_OTHER, response
        )
    }

    /**
     * If we are editing time, and missing an id, should throw inexact inputs
     */
    @Category(APITestCategory::class)
    @Test
    fun testEdit_Negative_MissingId() {
        val expected = MessageAPI.createCustomMessageRedirect("Integer must not be a null value", false, ViewTimeAPI.path)
        val data = PostBodyData(
            mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
                ViewTimeAPI.Elements.BEING_EDITED.getElemName() to "true",
            )
        )
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertEquals(expected, result)
    }


    /**
     * If a time period (future or past, doesn't matter) has been submitted, it isn't possible to
     * create a new time entry for it.
     */
    @Test
    fun testEdit_DateInvalid_DateEntryDisallowedForSubmittedTime() {
        val expected =
            MessageAPI.createEnumMessageRedirect(MessageAPI.Message.NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD)
        tru.isInASubmittedPeriodBehavior = { true }
        val data = PostBodyData(
            mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
                ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1",
                ViewTimeAPI.Elements.BEING_EDITED.getElemName() to "true",
            )
        )
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertEquals(expected, result)
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


    private fun enterTimeWithAPI(
        proj: String = "default project",
        time: String = "1",
        dtl: String = "not much to say",
        date: String = DEFAULT_DATE_STRING
    ): PreparedResponseData {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to proj,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to time,
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to dtl,
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to date))
        val sd = makeETServerData(data)

        return EnterTimeAPI.handlePost(sd)
    }

    /**
     * this should confirm what happens when a user successfully enters their time
     */
    private fun assertSuccessfulTimeEntry(result: PreparedResponseData) {
        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.any { it.matches(redirectRegex)})
    }

    companion object {
        val redirectRegex = """Location: timeentries\?date=....-..-..""".toRegex()

        val happyPathData = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
    }

    /**
     * Helper method for the kinds of [ServerData] we will
     * ordinarily see in entering time.
     */
    private fun makeETServerData(data: PostBodyData, user: User = DEFAULT_REGULAR_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = EnterTimeAPI.path)
    }

}
