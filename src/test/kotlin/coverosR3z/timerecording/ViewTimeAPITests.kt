package coverosR3z.timerecording

import coverosR3z.DEFAULT_DATE_STRING
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.misc.utility.toStr
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.ServerData
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.api.ViewTimeAPI
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
            ViewTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
            ViewTimeAPI.Elements.TIME_INPUT.elemName to "60",
            ViewTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
            ViewTimeAPI.Elements.ID_INPUT.elemName to "1"
        )
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED)
        val response = ViewTimeAPI.handlePost(sd).fileContents
        Assert.assertTrue(
            "we should have gotten the success page.  Got: $response",
            toStr(response).contains("SUCCESS")
        )
    }
}