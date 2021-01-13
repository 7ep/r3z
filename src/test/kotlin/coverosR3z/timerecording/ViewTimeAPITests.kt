package coverosR3z.timerecording

import coverosR3z.DEFAULT_DATE_STRING
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.utility.toStr
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.ServerData
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI.Elements
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
        val data = mapOf(
            Elements.PROJECT_INPUT.elemName to "1",
            Elements.TIME_INPUT.elemName to "60",
            Elements.DETAIL_INPUT.elemName to "not much to say",
            Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.elemName to "1"
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)
        val response = ViewTimeAPI.handlePost(sd).fileContents
        Assert.assertTrue(
            "we should have gotten the success page.  Got: $response",
            toStr(response).contains("SUCCESS")
        )
    }

    @Test
    fun testEditTime_Negative_MissingId() {
        val data = mapOf(
            Elements.PROJECT_INPUT.elemName to "1",
            Elements.TIME_INPUT.elemName to "60",
            Elements.DETAIL_INPUT.elemName to "not much to say",
            Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]. " +
                "received keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingProject() {
        val data = mapOf(
            Elements.TIME_INPUT.elemName to "60",
            Elements.DETAIL_INPUT.elemName to "not much to say",
            Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.elemName to "1"
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]. " +
                "received keys: [${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingTime() {
        val data = mapOf(
            Elements.PROJECT_INPUT.elemName to "1",
            Elements.DETAIL_INPUT.elemName to "not much to say",
            Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.elemName to "1"
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]. " +
                "received keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingDetail() {
        val data = mapOf(
            Elements.PROJECT_INPUT.elemName to "1",
            Elements.TIME_INPUT.elemName to "60",
            Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
            Elements.ID_INPUT.elemName to "1"
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]. " +
                "received keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]", ex.message)
    }

    @Test
    fun testEditTime_Negative_MissingDate() {
        val data = mapOf(
            Elements.PROJECT_INPUT.elemName to "1",
            Elements.TIME_INPUT.elemName to "60",
            Elements.DETAIL_INPUT.elemName to "not much to say",
            Elements.ID_INPUT.elemName to "1"
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(InexactInputsException::class.java){ ViewTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.DATE_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]. " +
                "received keys: [${Elements.PROJECT_INPUT.elemName}, ${Elements.TIME_INPUT.elemName}, ${Elements.DETAIL_INPUT.elemName}, ${Elements.ID_INPUT.elemName}]", ex.message)
    }


}