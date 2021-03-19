package coverosR3z.timerecording

import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.misc.DEFAULT_REGULAR_USER
import coverosR3z.misc.makeServerData
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.api.DeleteTimeAPI
import coverosR3z.timerecording.api.EditTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalStateException

class DeleteTimeAPITests {
    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // test deleting a time entry
    @Category(APITestCategory::class)
    @Test
    fun testDeleteTime() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeDTServerData(data)

        val response = DeleteTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, response)
    }

    // test deleting a non-existent time entry
    @Category(APITestCategory::class)
    @Test
    fun testDeleteTime_BadId() {
        tru.deleteTimeEntryBehavior = { throw IllegalStateException() }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeDTServerData(data)

        assertThrows(IllegalStateException::class.java) { DeleteTimeAPI.handlePost(sd) }
    }

    /**
     * Helper method to make a [ServerData] for the Edit time API tests
     */
    private fun makeDTServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, user = DEFAULT_REGULAR_USER)
    }
}