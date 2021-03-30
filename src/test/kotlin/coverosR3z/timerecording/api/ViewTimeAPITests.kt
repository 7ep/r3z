package coverosR3z.timerecording.api

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class ViewTimeAPITests {

    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        val pmd = PureMemoryDatabase.createEmptyDatabase()
        val cu = CurrentUser(DEFAULT_USER)
        au = AuthenticationUtilities(
            AuthenticationPersistence(pmd, logger = testLogger),
            testLogger,
        )
        tru = TimeRecordingUtilities(TimeEntryPersistence(pmd, logger = testLogger), cu, testLogger)
    }

    // region role tests

    @Category(APITestCategory::class)
    @Test
    fun testViewingTimeEntries_RegularUser() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)
        val result = ViewTimeAPI.handleGet(sd)
        assertEquals(StatusCode.OK, result.statusCode)
    }

    @Category(APITestCategory::class)
    @Test
    fun testViewingTimeEntries_System() {
        val sd = makeServerData(PostBodyData(), tru, au, user = SYSTEM_USER)
        val result = ViewTimeAPI.handleGet(sd)
        assertEquals(StatusCode.FORBIDDEN, result.statusCode)
    }

    @Category(APITestCategory::class)
    @Test
    fun testViewingTimeEntries_None() {
        val sd = makeServerData(PostBodyData(), tru, au, user = NO_USER)
        val result = ViewTimeAPI.handleGet(sd)
        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
    }

    @Category(APITestCategory::class)
    @Test
    fun testViewingTimeEntries_Admin() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_ADMIN_USER)
        val result = ViewTimeAPI.handleGet(sd)
        assertEquals(StatusCode.OK, result.statusCode)
    }

    @Category(APITestCategory::class)
    @Test
    fun testViewingTimeEntries_Approver() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_APPROVER)
        val result = ViewTimeAPI.handleGet(sd)
        assertEquals(StatusCode.OK, result.statusCode)
    }

    // endregion
}