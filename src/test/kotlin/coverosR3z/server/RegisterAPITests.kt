package coverosR3z.server

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.server.api.handleUnauthorized
import coverosR3z.server.types.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Tests related to registering a user through the API
 */
@Category(APITestCategory::class)
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
    @Test
    fun testShouldHandleValidInputs() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.value.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val response = RegisterAPI.handlePost(sd)

        assertEquals("The system should redirect to the login page.",
            StatusCode.OK,
            response.statusCode)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_blankName() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to "",
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    /**
     * If the invitation provided doesn't get us an employee
     */
    @Test
    fun testShouldHandleInvalidInputs_EmployeeNotFoundFromInvitation() {
        au.registerBehavior ={ RegistrationResult(RegistrationResultStatus.NO_INVITATION_FOUND, NO_USER) }
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to "does not matter, employee won't be found"))
        val sd = makeDefaultRegisterServerData(data)

        val result = RegisterAPI.handlePost(sd)

        assertEquals(handleUnauthorized(), result)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, invitation]. received keys: [invitation]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingInvitation() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value))
        val sd = makeDefaultRegisterServerData(data)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, invitation]. received keys: [username]", ex.message)
    }

    /**
     * If someone enters a username that has already been used,
     * return a message indicating so
     */
    @Test
    fun testRegisterDuplicateUsername() {
        au.registerBehavior = { RegistrationResult(RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, NO_USER) }
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.INVITATION_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.value.toString()))
        val sd = makeDefaultRegisterServerData(data)

        val result = RegisterAPI.handlePost(sd)

        assertTrue("The system should indicate that the username was already taken: " +
            result.headers.joinToString(";"), result.headers.contains("Location: register?invitation=1&msg=duplicate_user"))
    }

    /**
     * Happy path for GET of the registration page
     */
    @Test
    fun testRegisterGet() {
        au.getEmployeeFromInvitationCodeBehavior = { DEFAULT_EMPLOYEE }
        val sd = makeDefaultRegisterServerData(queryStringMap = mapOf(RegisterAPI.Elements.INVITATION_INPUT.getElemName() to "abc123"))

        val result = RegisterAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("""name="invitation" value="abc123""""))
    }

    /**
     * A helper to create the default [ServerData] that will exist during registration
     */
    private fun makeDefaultRegisterServerData(
        data: PostBodyData = PostBodyData(),
        queryStringMap: Map<String, String> = mapOf(),
    ): ServerData {
        return ServerData(
            BusinessCode(tru, au),
            fakeServerObjects,
            AnalyzedHttpData(data = data, user = NO_USER, queryString = queryStringMap),
            AuthStatus.UNAUTHENTICATED,
            testLogger
        )
    }

}