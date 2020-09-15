package coverosR3z.authentication

import coverosR3z.*
import coverosR3z.domainobjects.*
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

    val cua = CurrentUserAccessor() // since we have a method to clear this, we can share it between tests
    /**
     * The key here is that when we do this, it's
     * recording who does what, and only allows proper
     * people to carry out actions.  Like, Alice
     * can only modify Alice's stuff.
     */

    fun `given I am logged in`(pmd: PureMemoryDatabase) {
        cua.clearCurrentUserTestOnly() // We need to use cua for this test, sharing a pmd with auth persistence
        // and time recording persistence, in order to avoid recordTime throwing a USER_EMPLOYEE_MISMATCH status
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, 1) // we know DEFAULT_EMPLOYEE has an id=1
        au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)

        // Perform some quick checks
        val user = authPersistence.getUser(UserName(DEFAULT_USER.name))

        println("DEBUG")
        println(cua.get())

        assertEquals(user, cua.get()) // auth persistence and user persistence must agree
        assertTrue("our user should be registered", au.isUserRegistered(DEFAULT_USER.name)) // registration must succeed
    }
    @Test
    fun `I can add a time entry`() {
        // given I am logged in
        val pmd = PureMemoryDatabase()

        `given I am logged in`(pmd)

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd))
        tru.createEmployee(EmployeeName(DEFAULT_USER.name))
        tru.createProject(DEFAULT_PROJECT_NAME)

        println("DEBUG")
        println(cua.get())
        // when I add a time entry
//        val id = cua.get().employeeId ?: 1 // we should use the actual id of the employee in the following TimeEntry
        val entry = createTimeEntryPreDatabase(DEFAULT_EMPLOYEE)
        val result = tru.recordTime(entry)

        // then it proceeds successfully
        // TODO: this is known to fail, because employeeId for the registered user is null
        assertEquals(RecordTimeResult(StatusEnum.SUCCESS), result)
    }

    @Test
    fun `I cannot change someone else's time`() {
        val pmd = PureMemoryDatabase()
        `given I am logged in as jenna`(pmd)

        // an employee that is not jenna needs to exist
        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd))
        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        tru.createProject(DEFAULT_PROJECT_NAME)

        // when I try to add a time-entry for "not_jenna"
        val entry = createTimeEntryPreDatabase(DEFAULT_EMPLOYEE) // id=1 will belong to jenna, and
            // RecordTimeResult only compares for mismatch by id
        val result = tru.recordTime(entry)

        // then the system disallows it
        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH), result)
    }

    @Test
    fun `I should be able to register a employee with a valid password`() {
        // given I am not currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // when I register a new employee with employeename "matt" and password "asdfoiajwefowejf"
        au.register("matt", "asdfoiajwefowejf", 12, )

        // then the system records the registration successfully
        assertTrue("our user should be registered", au.isUserRegistered("matt"))
    }

    @Test
    fun `I should not be able to register a user if they are already registered`() {
        // given I am currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf", 12, )

        // when I register again with the same credentials
        val result = au.register("matt", "asdfoiajwefowejf", 12, )

        // then the system records the registration successfully
        assertEquals("we shouldn't be allowed to register a user again", RegistrationResult.ALREADY_REGISTERED, result)
    }

    @Test
    fun `I should be able to log in once I'm a registered user`() {
        // given I have registered
        cua.clearCurrentUserTestOnly()
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf", 12, )

        // when I enter valid credentials
        au.login("matt", "asdfoiajwefowejf")

        // then the system knows who I am
        val user = authPersistence.getUser(UserName("matt"))
        assertEquals(user, cua.get())
    }

    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        // given I have registered using "usera" and "password123"
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence, FakeCurrentUserAccessor())
        val regStatus = au.register("usera", "password1234", 12, )
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
        val regStatus = au.register("usera", "password123", 12, )

        // then the system denies the registration on the basis of a bad password
        assertEquals(RegistrationResult.PASSWORD_TOO_SHORT, regStatus)
    }


    private fun `given I am logged in as jenna`(pmd: PureMemoryDatabase) {
        cua.clearCurrentUserTestOnly()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)

        // registering a new user and logging in with them
        val username = "jenna"
        val password = "password12345"
        au.register(username, password, 12, )
        au.login(username, password)
    }
}
