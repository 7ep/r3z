package coverosR3z.timerecording

import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.misc.DEFAULT_PROJECT
import coverosR3z.misc.DEFAULT_REGULAR_USER
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.makeServerData
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.api.EditTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI.Elements
import coverosR3z.timerecording.types.NO_PROJECT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalStateException

@Category(APITestCategory::class)
class EditTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
        tru.findProjectByNameBehavior = { DEFAULT_PROJECT }
    }

    // test editing a time entry
    @Test
    fun testEditTime() {
        tru.findEmployeeByIdBehavior = { DEFAULT_REGULAR_USER.employee }
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
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

    /**
     * Only the employee who owns the time may submit the time.
     * If we detect it is some other employee doing so, throw an exception
     */
    @Test
    fun testEditTime_InvalidEmployee() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            Elements.TIME_INPUT.getElemName() to "1",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(IllegalStateException::class.java) { EditTimeAPI.handlePost(sd) }
        assertEquals("It is not allowed for anyone other than the owning employee to edit this time entry", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingId() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}]", ex.message)
    }

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

    @Test
    fun testEditTime_Negative_MissingTime() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingDetail() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ EditTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingDate() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
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
     * If a time period (future or past, doesn't matter) has been submitted, it isn't possible to
     * create a new time entry for it.
     */
    @Test
    fun testDateInvalid_DateEntryDisallowedForSubmittedTime() {
        tru.findEmployeeByIdBehavior = { DEFAULT_REGULAR_USER.employee }
        tru.isInASubmittedPeriodBehavior = { true }
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            Elements.TIME_INPUT.getElemName() to "1",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(IllegalStateException::class.java) { EditTimeAPI.handlePost(sd).statusCode }
        assertEquals("A time entry may not be edited in a submitted time period", ex.message)
    }

    
    /**
     * If we pass in an empty string for project
     */
    @Category(APITestCategory::class)
    @Test
    fun testEditTimeAPI_emptyStringProject() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(IllegalArgumentException::class.java) { EditTimeAPI.handlePost(sd) }
        assertEquals("Makes no sense to have an empty project name", ex.message)
    }

    /**
     * If we pass in all spaces as the project
     */
    @Category(APITestCategory::class)
    @Test
    fun testEditTimeAPI_allSpacesProject() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "   ",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(IllegalArgumentException::class.java) { EditTimeAPI.handlePost(sd) }
        assertEquals("Makes no sense to have an empty project name", ex.message)
    }

    /**
     * If the project passed in isn't recognized
     */
    @Category(APITestCategory::class)
    @Test
    fun testEditTimeAPI_unrecognizedProject() {
        tru.findProjectByNameBehavior = { NO_PROJECT }
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "UNRECOGNIZED",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(IllegalStateException::class.java) { EditTimeAPI.handlePost(sd) }
        assertEquals("Project with name of UNRECOGNIZED not found", ex.message)
    }


    /**
     * Helper method to make a [ServerData] for the Edit time API tests
     */
    private fun makeETServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, user = DEFAULT_REGULAR_USER)
    }

}