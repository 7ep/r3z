package coverosR3z.authentication

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.bddframework.BDD
import coverosR3z.misc.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.AnalyzedHttpData
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.timerecording.types.RecordTimeResult
import coverosR3z.timerecording.types.StatusEnum
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

class AuthenticationBDD {

    @BDD
    @Test
    fun `I cannot change someone else's time`() {
        val s = AuthenticationUserStory.getScenario("I cannot change someone else's time")

        val (tru, sarah) = initializeTwoUsersAndLogin()
        s.markDone("Given I am logged in as user alice and employees Sarah and Alice exist in the database,")

        val preparedEntry = createTimeEntryPreDatabase(sarah)
        val result = tru.createTimeEntry(preparedEntry)
        s.markDone("when I try to add a time-entry for Sarah,")

        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null), result)
        s.markDone("then the system disallows it.")
    }

    @BDD
    @Test
    fun `I should be able to register a user with a valid password`() {
        val s = AuthenticationUserStory.getScenario("I should be able to register a user with a valid password")

        val (pmd, au) = startWithEmptyDatabase()
        s.markDone("Given I am not currently registered,")

        registerANewUser(pmd, au)
        s.markDone("when I register a new user,")

        assertTrue("The user should be registered", au.isUserRegistered(DEFAULT_USER.name))
        s.markDone("then the system records that the registration succeeded.")
    }

    @BDD
    @Test
    fun `I should not be able to register a user if they are already registered`() {
        val s = AuthenticationUserStory.getScenario("I should not be able to register a user if they are already registered")

        val au = setupPreviousRegisteredUser()
        s.markDone("Given I have previously been registered,")

        val result = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        s.markDone("when I try to register again,")

        assertEquals("The user shouldn't be allowed to register again",
            RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, result.status)
        s.markDone("then the system records that the registration failed.")
    }

    @BDD
    @Test
    fun `I should be able to log in once I'm a registered user`() {
        val s = AuthenticationUserStory.getScenario("I should be able to log in once I'm a registered user")

        val (authPersistence, au) = setupPreviousRegistration()
        s.markDone("Given I have registered,")

        val (_, resultantUser) = au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        s.markDone("when I enter valid credentials,")

        assertSystemRecognizesUser(authPersistence, resultantUser)
        s.markDone("then the system knows who I am.")
    }

    @BDD
    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        val s = AuthenticationUserStory.getScenario("if I enter a bad password while logging in, I will be denied access")

        val au = doSuccessfulRegistration()
        s.markDone("Given I have registered,")

        val (status, _) = enterInvalidCredentials(au)
        s.markDone("when I login with the wrong credentials,")

        assertEquals(LoginResult.FAILURE, status)
        s.markDone("then the system denies me access.")
    }

    @BDD
    @Test
    fun `if I enter too short a password while registering, it will disallow it`() {
        val s = AuthenticationUserStory.getScenario("if I enter too short a password while registering, it will disallow it")

        val (_, au) = startWithEmptyDatabase()
        s.markDone("Given I am not registered,")

        val ex = enterTooShortPassword(au)
        s.markDone("when I register with too short of a password,")

        assertEquals(passwordMustBeLargeEnoughMsg, ex.message)
        s.markDone("then the system denies the registration on the basis of a bad password.")
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

    }

    private fun enterTooShortPassword(au: AuthenticationUtilities): IllegalArgumentException {
        val data =
            PostBodyData(mapOf("username" to DEFAULT_USER.name.value, "password" to "too short", "employee" to "1"))
        val sd = ServerData(
            au,
            FakeTimeRecordingUtilities(),
            AnalyzedHttpData(data = data, user = DEFAULT_USER),
            authStatus = AuthStatus.UNAUTHENTICATED,
            testLogger
        )
        return assertThrows(IllegalArgumentException::class.java) { RegisterAPI.handlePost(sd) }
    }

    private fun enterInvalidCredentials(au: AuthenticationUtilities) =
        au.login(DEFAULT_USER.name, Password("I'm not even trying to be a good password"))

    private fun doSuccessfulRegistration(): AuthenticationUtilities {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        val au = AuthenticationUtilities(authPersistence, testLogger)
        val regStatus = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(RegistrationResultStatus.SUCCESS, regStatus.status)
        return au
    }

    private fun assertSystemRecognizesUser(
        authPersistence: AuthenticationPersistence,
        resultantUser: User
    ) {
        val user = authPersistence.getUser(DEFAULT_USER.name)
        assertEquals(user, resultantUser)
    }

    private fun setupPreviousRegistration(): Pair<AuthenticationPersistence, AuthenticationUtilities> {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        val au = AuthenticationUtilities(authPersistence, testLogger)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        return Pair(authPersistence, au)
    }

    private fun setupPreviousRegisteredUser(): AuthenticationUtilities {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        val au = AuthenticationUtilities(authPersistence, testLogger)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        return au
    }

    private fun registerANewUser(
        pmd: PureMemoryDatabase,
        au: AuthenticationUtilities
    ) {
        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd, logger = testLogger), CurrentUser(SYSTEM_USER), testLogger)
        val employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, employee.id)
    }

    private fun startWithEmptyDatabase(): Pair<PureMemoryDatabase, AuthenticationUtilities> {
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd, testLogger)
        val au = AuthenticationUtilities(authPersistence, testLogger)
        return Pair(pmd, au)
    }


}
