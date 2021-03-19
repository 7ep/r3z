package coverosR3z.timerecording

import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.misc.DEFAULT_REGULAR_USER
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.makeServerData
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.api.EditTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI.Elements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class EditTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // test editing a time entry
    @Category(APITestCategory::class)
    @Test
    fun testEditTime() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "1",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)
        val response = EditTimeAPI.handlePost(sd).statusCode
        assertEquals(
            "We should have gotten redirected to the viewTime page",
            StatusCode.SEE_OTHER, response
        )
    }

    @Category(APITestCategory::class)
    @Test
    fun testEditTime_Negative_MissingId() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testEditTime_Negative_MissingProject() {
        val data = PostBodyData(mapOf(
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testEditTime_Negative_MissingTime() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testEditTime_Negative_MissingDetail() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testEditTime_Negative_MissingDate() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    /**
     * Helper method to make a [ServerData] for the Edit time API tests
     */
    private fun makeETServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, user = DEFAULT_REGULAR_USER)
    }

}