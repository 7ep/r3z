package coverosR3z.server

import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.passwordMustNotBeBlankMsg
import coverosR3z.authentication.types.usernameCannotBeEmptyMsg
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.DEFAULT_EMPLOYEE
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.misc.DEFAULT_USER
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.testLogger
import coverosR3z.server.types.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.employeeIdCannotBeBlank
import coverosR3z.timerecording.types.minEmployeeIdMsg
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

    lateinit var au : IAuthenticationUtilities
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
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.value.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

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
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_blankPassword() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to "",
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(passwordMustNotBeBlankMsg, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_blankEmployee() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to ""))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(employeeIdCannotBeBlank, ex.message)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_nonNumericEmployee() {
        val employee = "abc"
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to employee))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("Must be able to parse $employee as integer", ex.message)
    }

    /**
     * If the employee id is below zero
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_NegativeEmployee() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to "-10"))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals(minEmployeeIdMsg, ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password, employee]. received keys: [password, employee]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_missingPassword() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password, employee]. received keys: [username, employee]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldHandleInvalidInputs_missingEmployee() {
        val data = PostBodyData(mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.getElemName() to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.getElemName() to DEFAULT_PASSWORD.value))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED, testLogger)

        val ex = assertThrows(InexactInputsException::class.java){ RegisterAPI.handlePost(sd) }

        assertEquals("expected keys: [username, password, employee]. received keys: [username, password]", ex.message)
    }


}