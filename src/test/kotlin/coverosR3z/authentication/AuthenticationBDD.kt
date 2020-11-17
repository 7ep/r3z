package coverosR3z.authentication

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.server.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 *
 * As a employee,
 * I want to be able to securely use the system,
 * so that I know my time entries are confidential and cannot be manipulated by others
 */
class AuthenticationBDD {

    @Test
    fun `I cannot change someone else's time`() {
        // Given I am logged in as user "alice" and employees Sarah and Alice exist in the database
        val (tru, sarah) = initializeTwoUsersAndLogin()

        // When I try to add a time-entry for Sarah
        val preparedEntry = createTimeEntryPreDatabase(sarah)
        val result = tru.recordTime(preparedEntry)

        // Then the system disallows it
        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH), result)
    }

    @Test
    fun `I should be able to register a user with a valid password`() {
        // Given I am not currently registered
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
        val employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)

        // When I register a new user
        au.register(DEFAULT_USER.name.value, DEFAULT_PASSWORD, employee.id.value)

        // Then the system records that the registration succeeded
        assertTrue("The user should be registered", au.isUserRegistered(DEFAULT_USER.name.value))
    }

    @Test
    fun `I should not be able to register a user if they are already registered`() {
        // Given I have previously been registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name.value, DEFAULT_PASSWORD)

        // When I try to register again
        val result = au.register(DEFAULT_USER.name.value, DEFAULT_PASSWORD)

        // Then the system records that the registration failed
        assertEquals("The user shouldn't be allowed to register again", RegistrationResult.FAILURE, result)
    }

    @Test
    fun `I should be able to log in once I'm a registered user`() {
        // Given I have registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name.value, DEFAULT_PASSWORD)

        // When I enter valid credentials
        val (_, resultantUser) = au.login(DEFAULT_USER.name.value, DEFAULT_PASSWORD)

        // Then the system knows who I am
        val user = authPersistence.getUser(DEFAULT_USER.name)
        assertEquals(user, resultantUser)
    }

    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        // Given I have registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        val regStatus = au.register(DEFAULT_USER.name.value, DEFAULT_PASSWORD)
        assertEquals(RegistrationResult.SUCCESS, regStatus)

        // When I login with the wrong credentials
        val (status, _) = au.login(DEFAULT_USER.name.value, "I'm not even trying to be a good password")

        // Then the system denies me access
        assertEquals(LoginResult.FAILURE, status)
    }

    @Test
    fun `if I enter an invalid password while registering, it will disallow it`() {
        // Given I am not registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // When I register with too short of a a password
        val regStatus = au.register(DEFAULT_USER.name.value, "too short")

        // Then the system denies the registration on the basis of a bad password
        assertEquals(RegistrationResult.FAILURE, regStatus)
    }

    /**
     * When I login correctly with my browser, the system should reply with a header that sets
     * a cookie like sessionId=abc123, where abc123 is the session identifier in
     * the database.  This is effectively a machine-generated password that I can then
     * use across the system to hit authenticated pages
     */
    @Test
    fun `when I login successfully, persistent authentication is enabled`() {
        // Given I have registered
        val (su, requestData, _) = registerUser()

        // When I enter valid credentials
        val responseData: PreparedResponseData = su.handleRequestAndRespond(requestData)

        // Then the system provides a cookie to enable authenticated use
        assertTrue("headers should contain cookie.  Headers were: ${responseData.headers}", responseData.headers.any{h -> h.startsWith("Set-Cookie: sessionId=")})
    }



    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    companion object {
        private fun initializeTwoUsersAndLogin(): Pair<TimeRecordingUtilities, Employee> {
            val (tru, _) = initializeAUserAndLogin()
            val sarah = tru.createEmployee(EmployeeName("Sarah")) // Sarah will have id=2

            return Pair(tru, sarah)
        }

        fun registerUser(): Triple<ServerUtilities, RequestData, PureMemoryDatabase> {
            val pmd = PureMemoryDatabase()
            val authPersistence = AuthenticationPersistence(pmd)
            val au = AuthenticationUtilities(authPersistence)
            val regStatus = au.register(DEFAULT_USER.name.value, DEFAULT_PASSWORD)
            assertEquals(RegistrationResult.SUCCESS, regStatus)
            val tru = FakeTimeRecordingUtilities()
            val su = ServerUtilities(au, tru)
            val postedData = mapOf("username" to DEFAULT_USER.name.value, "password" to DEFAULT_PASSWORD)
            val requestData = RequestData(Verb.POST, NamedPaths.LOGIN.path, postedData, NO_USER)
            return Triple(su, requestData, pmd)
        }
    }


}
