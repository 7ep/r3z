package coverosR3z.authentication

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.NO_USER
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.ITimeRecordingUtilities
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException

class LoginAPITests {

    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * Happy path - all values provided as needed
     */
    @Test
    fun testHandlePostLogin_happyPath() {
        val data = mapOf(
                "username" to DEFAULT_USER.name.value,
                "password" to DEFAULT_PASSWORD)
        val responseData = handlePOSTLogin(au, NO_USER, data).fileContents
        Assert.assertTrue("The system should indicate success.  File was $responseData",
                responseData.contains("SUCCESS"))
    }

    /**
     * Happy path - all values provided as needed
     */
    @Test
    fun testHandlePostLogin_missingUser() {
        val data = mapOf("password" to DEFAULT_PASSWORD)
        val ex = assertThrows(IllegalArgumentException::class.java){handlePOSTLogin(au, NO_USER, data).fileContents}
        assertEquals("blah blah blah", ex.message)
    }

}