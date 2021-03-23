package coverosR3z.authentication.api

import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.DEFAULT_REGULAR_USER
import coverosR3z.misc.makeServerData
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class ChangePasswordAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * Happy path - all values provided as needed
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePost_happyPath() {
        val sd = makeServerData(PostBodyData(emptyMap()), tru, au, user = DEFAULT_REGULAR_USER)

        val resultStatusCode = ChangePasswordAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.OK, resultStatusCode)
    }

}