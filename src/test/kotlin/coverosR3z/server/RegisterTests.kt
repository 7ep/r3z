package coverosR3z.server

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.ITimeRecordingUtilities
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests related to registering a user
 */
class RegisterTests {

    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities
    lateinit var su : ServerUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
        su = ServerUtilities(au, tru)
    }

    /**
     * If our code received all the expected
     * values properly, it shouldn't complain
     */
    @Test
    fun testShouldHandleValidInputs() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val responseData = su.handleRequestAndRespond(rd)
        assertTrue("The system should indicate success.  File was ${responseData.fileContents}",
                responseData.fileContents.contains("SUCCESS"))
    }
}