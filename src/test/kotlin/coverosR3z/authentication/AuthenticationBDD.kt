package coverosR3z.authentication

import coverosR3z.*
import coverosR3z.domainobjects.RecordTimeResult
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.StatusEnum
import coverosR3z.domainobjects.UserName
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.timerecording.TimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import coverosR3z.domainobjects.LoginStatuses as ls

/**
 *
 * As a employee,
 * I want to be able to securely use the system,
 * so that I know my time entries are confidential and cannot be manipulated by others
 */
class AuthenticationBDD {

    /**
     * The key here is that when we do this, it's
     * recording who does what, and only allows proper
     * people to carry out actions.  Like, Alice
     * can only modify Alice's stuff.
     */
    @Test
    fun `I can add a time entry`() {
        // given I am logged in
        // when I add a time entry
        // then it proceeds successfully
    }

    @Test
    fun `I cannot change someone else's time`() {
        val tru = `given I am logged in as jenna`()

        // when I try to add a time-entry for "not_jenna"
        val entry = createTimeEntryPreDatabase(employee = DEFAULT_EMPLOYEE)
        val result = tru.recordTime(entry)

        // then the system disallows it
        assertEquals(RecordTimeResult(id = null, status = StatusEnum.USER_EMPLOYEE_MISMATCH), result)
    }

    @Test
    fun `I should be able to register a employee with a valid password`() {
        // given I am not currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // when I register a new employee with employeename "matt" and password "asdfoiajwefowejf"
        au.register("matt", "asdfoiajwefowejf")

        // then the system records the registration successfully
        assertTrue("our user should be registered", au.isUserRegistered("matt"))
    }

    @Test
    fun `I should not be able to register a employee if they are already registered`() {
        // given I am currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf")

        // when I register again with the same credentials
        val result = au.register("matt", "asdfoiajwefowejf")

        // then the system records the registration successfully
        assertEquals("we shouldn't be allowed to register a user again", RegistrationResult.ALREADY_REGISTERED, result)
    }

    @Test
    fun `I should be able to log in once I'm a registered employee`() {
        // given I have registered
        CurrentUser.clearCurrentUserTestOnly()
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf")

        // when I enter valid credentials
        au.login("matt", "asdfoiajwefowejf")

        // then the system knows who I am
        val user = authPersistence.getUser(UserName("matt"))
        assertEquals(user, CurrentUser.get())
    }

    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        // given I have registered using "usera" and "password123"
        CurrentUser.clearCurrentUserTestOnly()
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        val regStatus = au.register("usera", "password1234")
        assertEquals(RegistrationResult.SUCCESS, regStatus)

        // when I login with "usera" and "not_right_password"
        val status = au.login("usera", "not_right_password").status

        // then the system denies me access
        assertEquals(ls.FAILURE, status)
    }

    @Test
    fun `if I enter an invalid password while registering, it will disallow it`() {
        // given I am unregistered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // when I register with "usera" and "short" as password
        val regStatus = au.register("usera", "password123")

        // then the system denies the registration on the basis of a bad password
        assertEquals(RegistrationResult.PASSWORD_TOO_SHORT, regStatus)
    }


    private fun `given I am logged in as jenna`(): TimeRecordingUtilities {
        // clearing the current user
        CurrentUser.clearCurrentUserTestOnly()

        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // registering a new user and logging in with them
        val username = "jenna"
        val password = "password12345"
        au.register(username, password)
        au.login(username, password)

        // preparing so we can enter time with this employee and project
        val tru = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()))
        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        tru.createProject(DEFAULT_PROJECT_NAME)
        return tru
    }
}
