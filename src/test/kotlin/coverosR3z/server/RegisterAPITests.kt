package coverosR3z.server

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.DEFAULT_EMPLOYEE
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.misc.DEFAULT_USER
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.makeServerData
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Tests related to registering a user through the API
 */
class RegisterAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * If our code received all the expected
     * values properly, it shouldn't complain
     * Basically a happy path
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleValidInputs() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.value.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val response = RegisterAPI.handlePost(sd)

        assertEquals("The system should redirect to the login page.",
            StatusCode.SEE_OTHER,
            response.statusCode)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_blankName() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to "",
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_blankPassword() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to "",
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(passwordMustNotBeBlankMsg, ex.message)
    }

    /**
     * If the invitation provided doesn't get us an employee
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_EmployeeNotFoundFromInvitation() {
        au.registerBehavior ={ RegistrationResult(RegistrationResultStatus.NO_INVITATION_FOUND, NO_USER) }
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to "does not matter, employee won't be found"))
        val sd = makeDefaultRegisterServerData(data)

        val result = RegisterAPI.handlePost(sd)

        assertEquals(handleUnauthorized(), result)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password, invitation]. received keys: [password, invitation]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_missingPassword() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password, invitation]. received keys: [username, invitation]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_missingInvitation() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password, invitation]. received keys: [username, password]", ex.message)
    }

    /**
     * A helper to create the default [ServerData] that will exist during registration
     */
    private fun makeDefaultRegisterServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, AuthStatus.UNAUTHENTICATED, user = NO_USER)
    }


}