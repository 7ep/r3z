package coverosR3z.authentication

import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.types.LoginResult
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.passwordMustNotBeBlankMsg
import coverosR3z.authentication.types.usernameCannotBeEmptyMsg
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.utility.toStr
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.utility.doPOSTRequireUnauthenticated
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
                LoginAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            LoginAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)

        val responseData = doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) }

        Assert.assertTrue("The system should indicate success.  File was $responseData",
                toStr(responseData.fileContents).contains("SUCCESS"))
        Assert.assertTrue("A cookie should be set if valid login",
                responseData.headers.any { it.contains("Set-Cookie") })
    }

    @Test
    fun testHandlePostLogin_failedLogin() {
        au.loginBehavior = {Pair(LoginResult.FAILURE, NO_USER)}
        val data = mapOf(
            LoginAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            LoginAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)

        val responseData = doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) }

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
            LoginAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)

        val ex = assertThrows(InexactInputsException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) } }

        assertEquals("expected keys: [username, password]. received keys: [password]", ex.message)
    }

    /**
     * Error path - username is blank
     */
    @Test
    fun testHandlePostLogin_blankUser() {
        val data = mapOf(
            LoginAPI.Elements.USERNAME_INPUT.elemName to "",
            LoginAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)

        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) } }

        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    /**
     * Error path - password is not passed in
     */
    @Test
    fun testHandlePostLogin_missingPassword() {
        val data = mapOf(
            LoginAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value)

        val ex = assertThrows(InexactInputsException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) } }

        assertEquals("expected keys: [username, password]. received keys: [username]", ex.message)
    }

    /**
     * Error path - password is blank
     */
    @Test
    fun testHandlePostLogin_blankPassword() {
        val data = mapOf(
            LoginAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            LoginAPI.Elements.PASSWORD_INPUT.elemName to "")

        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, LoginAPI.requiredInputs, data) { LoginAPI.handlePOST(au, data) } }

        assertEquals(passwordMustNotBeBlankMsg, ex.message)

    }

}