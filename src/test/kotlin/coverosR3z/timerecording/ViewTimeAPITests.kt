package coverosR3z.timerecording

import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.utility.toStr
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI.Elements
import coverosR3z.timerecording.types.StatusEnum
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ViewTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // test editing a time entry
    @Test
    fun testEditTime() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)
        val response = ViewTimeAPI.handlePost(sd).statusCode
        assertEquals(
            "we should have gotten redirected to the viewTime page.  Got: $response",
            StatusCode.SEE_OTHER, response
        )
    }

    @Test
    fun testEditTime_Negative_MissingId() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
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
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingTime() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingDetail() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingDate() {
        val data = PostBodyData(mapOf(
            Elements.PROJECT_INPUT.getElemName() to "1",
            Elements.TIME_INPUT.getElemName() to "60",
            Elements.DETAIL_INPUT.getElemName() to "not much to say",
            Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.DATE_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]. " +
                "received keys: [${Elements.PROJECT_INPUT.getElemName()}, ${Elements.TIME_INPUT.getElemName()}, ${Elements.DETAIL_INPUT.getElemName()}, ${Elements.ID_INPUT.getElemName()}]", ex.message)
    }


}