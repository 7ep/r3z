package coverosR3z.authentication

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.bdd.AuthenticationUserStory
import coverosR3z.bdd.BDD
import coverosR3z.bdd.BDDHelpers
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
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class AuthenticationBDD {

    @BDD
    @Test
    fun `I cannot change someone else's time`() {
        val s = AuthenticationUserStory.addScenario(
            "I cannot change someone else's time",

            listOf(
                "Given I am logged in as user alice and employees Sarah and Alice exist in the database,",
                "when I try to add a time-entry for Sarah,",
                "then the system disallows it."
            )
        )

        val (tru, sarah) = initializeTwoUsersAndLogin()
        s.markDone("Given I am logged in as user alice and employees Sarah and Alice exist in the database,")

        val preparedEntry = createTimeEntryPreDatabase(sarah)
        val result = tru.recordTime(preparedEntry)
        s.markDone("when I try to add a time-entry for Sarah,")

        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null), result)
        s.markDone("then the system disallows it.")
    }

    @BDD
    @Test
    fun `I should be able to register a user with a valid password`() {
        val s = AuthenticationUserStory.addScenario(
            "I should be able to register a user with a valid password",

            listOf(
                "Given I am not currently registered,",
                "when I register a new user,",
                "then the system records that the registration succeeded."
            )
        )

        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)
        s.markDone("Given I am not currently registered,")

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
        val employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, employee.id)
        s.markDone("when I register a new user,")

        assertTrue("The user should be registered", au.isUserRegistered(DEFAULT_USER.name))
        s.markDone("then the system records that the registration succeeded.")
    }

    @BDD
    @Test
    fun `I should not be able to register a user if they are already registered`() {
        val s = AuthenticationUserStory.addScenario(
            "I should not be able to register a user if they are already registered",

            listOf(
                "Given I have previously been registered,",
                "when I try to register again,",
                "then the system records that the registration failed."
            )
        )

        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
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
        val s = AuthenticationUserStory.addScenario(
            "I should be able to log in once I'm a registered user",

            listOf(
                "Given I have registered,",
                "when I enter valid credentials,",
                "then the system knows who I am."
            )
        )

        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        s.markDone("Given I have registered,")

        val (_, resultantUser) = au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        s.markDone("when I enter valid credentials,")

        val user = authPersistence.getUser(DEFAULT_USER.name)
        assertEquals(user, resultantUser)
        s.markDone("then the system knows who I am.")
    }

    @BDD
    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        val s = AuthenticationUserStory.addScenario(
            "if I enter a bad password while logging in, I will be denied access",

            listOf(
                "Given I have registered,",
                "when I login with the wrong credentials,",
                "then the system denies me access."
            )
        )

        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        val regStatus = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(RegistrationResultStatus.SUCCESS, regStatus.status)
        s.markDone("Given I have registered,")

        val (status, _) = au.login(DEFAULT_USER.name, Password("I'm not even trying to be a good password"))
        s.markDone("when I login with the wrong credentials,")

        assertEquals(LoginResult.FAILURE, status)
        s.markDone("then the system denies me access.")
    }

    @BDD
    @Test
    fun `if I enter an invalid password while registering, it will disallow it`() {
        val s = AuthenticationUserStory.addScenario(
            "if I enter an invalid password while registering, it will disallow it",

            listOf(
                "Given I am not registered,",
                "when I register with too short of a password,",
                "then the system denies the registration on the basis of a bad password."
            )
        )

        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        s.markDone("Given I am not registered,")

        val data = PostBodyData(mapOf("username" to DEFAULT_USER.name.value, "password" to "too short", "employee" to "1"))
        val sd = ServerData(au, FakeTimeRecordingUtilities(), AnalyzedHttpData(data = data, user = DEFAULT_USER), authStatus = AuthStatus.UNAUTHENTICATED)
        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePost(sd)}
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

        private lateinit var b : BDDHelpers

        @BeforeClass
        @JvmStatic
        fun init() {
            b = BDDHelpers("authenticationBDD.html")
        }

        @AfterClass
        @JvmStatic
        fun finishing() {
            b.writeToFile()
        }

        private fun initializeTwoUsersAndLogin(): Pair<TimeRecordingUtilities, Employee> {
            val (tru, _) = initializeAUserAndLogin()
            val sarah = tru.createEmployee(EmployeeName("Sarah")) // Sarah will have id=2

            return Pair(tru, sarah)
        }

    }


}
