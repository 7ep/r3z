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

    private val cua = CurrentUserAccessor() // since we have a method to clear this, we can share it between tests

    fun initializeTwoUsersAndLogin() : Pair<TimeRecordingUtilities, Employee>{
        // We need to use cua for this test, sharing a pmd with auth persistence
        // and time recording persistence, in order to avoid recordTime throwing a USER_EMPLOYEE_MISMATCH status
        val (tru, _) = initializeAUserAndLogin()
        val sarah = tru.createEmployee(EmployeeName("Sarah")) // Sarah will have id=2

        return Pair(tru, sarah)
    }

    fun initializeAUserAndLogin() : Pair<TimeRecordingUtilities, Employee>{

        cua.clearCurrentUserTestOnly() // We need to use cua for this test, sharing a pmd with auth persistence
        // and time recording persistence, in order to avoid recordTime throwing a USER_EMPLOYEE_MISMATCH status
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)
        au.register("alice", DEFAULT_PASSWORD, 1)
        au.login("alice", DEFAULT_PASSWORD)

        // Perform some quick checks
        assertEquals("Auth persistence and user persistence must agree",
                authPersistence.getUser(UserName("alice")), cua.get())
        assertTrue("Registration must have succeeded", au.isUserRegistered("alice"))

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd))
        val alice = tru.createEmployee(EmployeeName("Alice"))
        tru.createProject(DEFAULT_PROJECT_NAME)

        return Pair(tru, alice)
    }

    @Test
    fun `I can add a time entry`() {
        // Given I am logged in
        val (tru, _) = initializeAUserAndLogin()

        // When I add a time entry
        val entry = createTimeEntryPreDatabase(Employee(1, "Alice"))
        val result = tru.recordTime(entry)

        // Then it proceeds successfully
        assertEquals(RecordTimeResult(StatusEnum.SUCCESS), result)
    }

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
    fun `I should be able to register a employee with a valid password`() {
        // Given I am not currently registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // When I register a new user
        au.register("matt", "asdfoiajwefowejf", 12)

        // Then the system records that the registration succeeded
        assertTrue("The user should be registered", au.isUserRegistered("matt"))
    }

    @Test
    fun `I should not be able to register a user if they are already registered`() {
        // Given I have previously been registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf", 12)

        // When I try to register again
        val result = au.register("matt", "asdfoiajwefowejf", 12)

        // Then the system records that the registration failed
        assertEquals("The user shouldn't be allowed to register again", RegistrationResult.ALREADY_REGISTERED, result)
    }

    @Test
    fun `I should be able to log in once I'm a registered user`() {
        // Given I have registered
        cua.clearCurrentUserTestOnly()
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register("matt", "asdfoiajwefowejf", 12)

        // When I enter valid credentials
        au.login("matt", "asdfoiajwefowejf")

        // Then the system knows who I am
        val user = authPersistence.getUser(UserName("matt"))
        assertEquals(user, cua.get())
    }

    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        // Given I have registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence, FakeCurrentUserAccessor())
        val regStatus = au.register("matt", "asdfoiajwefowejf", 12)
        assertEquals(RegistrationResult.SUCCESS, regStatus)

        // When I login with the wrong credentials
        val status = au.login("matt", "I'm not even trying to be a good password").status

        // Then the system denies me access
        assertEquals(ls.FAILURE, status)
    }

    @Test
    fun `if I enter an invalid password while registering, it will disallow it`() {
        // Given I am not registered
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)

        // When I register with too short of a a password
        val regStatus = au.register("matt", "too short", 12)

        // Then the system denies the registration on the basis of a bad password
        assertEquals(RegistrationResult.PASSWORD_TOO_SHORT, regStatus)
    }
}
