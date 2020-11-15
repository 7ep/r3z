package coverosR3z.server

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests related to registering a user
 */
class RegisterTests {

    /**
     * If our code received all the expected
     * values properly, it shouldn't complain
     */
    @Test
    fun testShouldHandleValidInputs() {
        val au = FakeAuthenticationUtilities()
        val tru = FakeTimeRecordingUtilities()
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf(
                        "username" to DEFAULT_USER.name,
                        "password" to DEFAULT_PASSWORD,
                        "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val responseData = ServerUtilities(au, tru).handleRequestAndRespond(rd)
        assertTrue(
                "The system should indicate success.  File was ${responseData.fileContents}",
                responseData.fileContents.contains("SUCCESS"))
    }
}