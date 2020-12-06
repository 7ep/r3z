package coverosR3z.authentication

import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.InexactInputsException
import coverosR3z.misc.toStr
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class LoginAPITests {

    lateinit var au : FakeAuthenticationUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
    }

    /**
     * Happy path - all values provided as needed
     */
    @Test
    fun testHandlePostLogin_happyPath() {
        val data = mapOf(
                LoginElements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
                LoginElements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val responseData = handlePOSTLogin(au, NO_USER, data)
        Assert.assertTrue("The system should indicate success.  File was $responseData",
                toStr(responseData.fileContents).contains("SUCCESS"))
        Assert.assertTrue("A cookie should be set if valid login",
                responseData.headers.any { it.contains("Set-Cookie") })
    }

    @Test
    fun testHandlePostLogin_failedLogin() {
        au.loginBehavior = {Pair(LoginResult.FAILURE, NO_USER)}
        val data = mapOf(
                LoginElements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
                LoginElements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val responseData = handlePOSTLogin(au, NO_USER, data)
        Assert.assertTrue("The system should indicate failure.  File was ${toStr(responseData.fileContents)}",
                toStr(responseData.fileContents).contains("401 error"))
        Assert.assertTrue("A cookie should be set if valid login",
                responseData.headers.none { it.contains("Set-Cookie") })
    }

    /**
     * Error path - username is not passed in
     */
    @Test
    fun testHandlePostLogin_missingUser() {
        val data = mapOf(
                LoginElements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTLogin(au, NO_USER, data)}
        assertEquals("expected keys: [username, password]. received keys: [password]", ex.message)
    }

    /**
     * Error path - username is blank
     */
    @Test
    fun testHandlePostLogin_blankUser() {
        val data = mapOf(
                LoginElements.USERNAME_INPUT.elemName to "",
                LoginElements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val ex = assertThrows(IllegalArgumentException::class.java) { handlePOSTLogin(au, NO_USER, data) }
        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    /**
     * Error path - password is not passed in
     */
    @Test
    fun testHandlePostLogin_missingPassword() {
        val data = mapOf(
            LoginElements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value)
        val ex = assertThrows(InexactInputsException::class.java){handlePOSTLogin(au, NO_USER, data)}
        assertEquals("expected keys: [username, password]. received keys: [username]", ex.message)
    }

    /**
     * Error path - password is blank
     */
    @Test
    fun testHandlePostLogin_blankPassword() {
        val data = mapOf(
            LoginElements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            LoginElements.PASSWORD_INPUT.elemName to "")
        val ex = assertThrows(java.lang.IllegalArgumentException::class.java) { handlePOSTLogin(au, NO_USER, data) }
        assertEquals(passwordMustNotBeBlankMsg, ex.message)

    }

}