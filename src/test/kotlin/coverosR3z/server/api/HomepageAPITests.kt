package coverosR3z.server.api

import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.DEFAULT_ADMIN_USER
import coverosR3z.misc.DEFAULT_APPROVER
import coverosR3z.misc.DEFAULT_REGULAR_USER
import coverosR3z.misc.makeServerData
import coverosR3z.misc.utility.toStr
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class HomepageAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A basic happy path - an administrator GETing the page
     */
    @Category(APITestCategory::class)
    @Test
    fun testGetAsAdmin() {
        val sd = makeServerData(user = DEFAULT_ADMIN_USER, data = PostBodyData(), tru = tru, au = au)

        val result = toStr(HomepageAPI.handleGet(sd).fileContents)

        assertTrue(result.contains("Create employee"))
        assertTrue(result.contains("Create project"))
        assertTrue(result.contains("Time entries"))
        assertTrue(result.contains("Log configuration"))
    }

    /**
     * If a regular user asks for the homepage, they get redirected to
     * viewing time entries
     */
    @Category(APITestCategory::class)
    @Test
    fun testGetAsRegularUser() {
        val sd = makeServerData(user = DEFAULT_REGULAR_USER, data = PostBodyData(), tru = tru, au = au)

        val result = HomepageAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    /**
     * If a regular user asks for the homepage, they get redirected to
     * viewing time entries
     */
    @Category(APITestCategory::class)
    @Test
    fun testGetAsApproverUser() {
        val sd = makeServerData(user = DEFAULT_APPROVER, data = PostBodyData(), tru = tru, au = au)

        val result = HomepageAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }
}