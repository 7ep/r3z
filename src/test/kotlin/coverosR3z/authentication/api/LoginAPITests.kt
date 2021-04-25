package coverosR3z.authentication.api

import coverosR3z.authentication.types.LoginResult
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.passwordMustNotBeBlankMsg
import coverosR3z.authentication.types.usernameCannotBeEmptyMsg
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.system.misc.DEFAULT_PASSWORD
import coverosR3z.system.misc.DEFAULT_USER
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.system.misc.makeServerData
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class LoginAPITests {

    lateinit var au : FakeAuthenticationUtilities
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
        val data = PostBodyData(mapOf(
                LoginAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            LoginAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value))

        val sd = makeLoginServerData(data)
        val responseData = LoginAPI.handlePost(sd)

        assertEquals("The system should take you to the authenticated homepage",
                StatusCode.SEE_OTHER, responseData.statusCode)
        assertTrue("A cookie should be set if valid login",
                responseData.headers.any { it.contains("Set-Cookie") })
    }

    @Test
    fun testHandlePostLogin_failedLogin() {
        au.loginBehavior = {Pair(LoginResult.FAILURE, NO_USER)}
        val data = PostBodyData(mapOf(
            LoginAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            LoginAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value))

        val sd = makeLoginServerData(data)
        val result = LoginAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.joinToString(";"), result.headers.contains("Location: result?msg=FAILED_LOGIN"))
        assertTrue("A cookie should not be set if failed login",
                result.headers.none { it.contains("Set-Cookie") })
    }

    /**
     * Error path - username is not passed in
     */
    @Test
    fun testHandlePostLogin_missingUser() {
        val data = PostBodyData(mapOf(
            LoginAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value))

        val sd = makeLoginServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ LoginAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password]. received keys: [password]", ex.message)
    }

    /**
     * Error path - username is blank
     */
    @Test
    fun testHandlePostLogin_blankUser() {
        val data = PostBodyData(mapOf(
            LoginAPI.Elements.USERNAME_INPUT.getElemName() to "",
            LoginAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value))

        val sd = makeLoginServerData(data)
        val ex = assertThrows(IllegalArgumentException::class.java){ LoginAPI.handlePost(sd) }

        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    /**
     * Error path - password is not passed in
     */
    @Test
    fun testHandlePostLogin_missingPassword() {
        val data = PostBodyData(mapOf(
            LoginAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value))

        val sd = makeLoginServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ LoginAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password]. received keys: [username]", ex.message)
    }

    /**
     * Error path - password is blank
     */
    @Test
    fun testHandlePostLogin_blankPassword() {
        val data = PostBodyData(mapOf(
            LoginAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            LoginAPI.Elements.PASSWORD_INPUT.getElemName() to ""))

        val sd = makeLoginServerData(data)
        val ex = assertThrows(IllegalArgumentException::class.java){ LoginAPI.handlePost(sd) }

        assertEquals(passwordMustNotBeBlankMsg, ex.message)

    }

    /**
     * A helper method for the ordinary [ServerData] present during login
     */
    private fun makeLoginServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, AuthStatus.UNAUTHENTICATED, user = NO_USER)
    }

}