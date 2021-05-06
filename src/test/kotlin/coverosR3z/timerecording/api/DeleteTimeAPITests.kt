package coverosR3z.timerecording.api

import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.system.misc.DEFAULT_REGULAR_USER
import coverosR3z.system.misc.makeServerData
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class DeleteTimeAPITests {
    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // test deleting a time entry
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
     * Helper method to make a [ServerData] for the delete time API tests
     */
    private fun makeDTServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, user = DEFAULT_REGULAR_USER)
    }
}