package coverosR3z.authentication

import coverosR3z.*
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.domainobjects.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
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
        val (tru, sarah) = initializeTwoUsersAndLogin()
        b.markDone("Given I am logged in as user alice and employees Sarah and Alice exist in the database,")

        val preparedEntry = createTimeEntryPreDatabase(sarah)
        val result = tru.recordTime(preparedEntry)
        b.markDone("when I try to add a time-entry for Sarah,")

        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null), result)
        b.markDone("then the system disallows it.")
    }

    @Test
    fun `I should be able to register a user with a valid password`() {
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)
        b.markDone("Given I am not currently registered,")

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
        val employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)

        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, employee.id)
        b.markDone("when I register a new user,")

        assertTrue("The user should be registered", au.isUserRegistered(DEFAULT_USER.name))
        b.markDone("then the system records that the registration succeeded.")
    }

    @Test
    fun `I should not be able to register a user if they are already registered`() {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        b.markDone("Given I have previously been registered,")

        val result = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        b.markDone("when I try to register again,")

        assertEquals("The user shouldn't be allowed to register again",
            RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, result.status)
        b.markDone("then the system records that the registration failed.")
    }

    @Test
    fun `I should be able to log in once I'm a registered user`() {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        b.markDone("Given I have registered,")

        val (_, resultantUser) = au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        b.markDone("when I enter valid credentials,")

        val user = authPersistence.getUser(DEFAULT_USER.name)
        assertEquals(user, resultantUser)
        b.markDone("then the system knows who I am.")
    }

    @Test
    fun `if I enter a bad password while logging in, I will be denied access`() {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        val regStatus = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(RegistrationResultStatus.SUCCESS, regStatus.status)
        b.markDone("Given I have registered,")

        val (status, _) = au.login(DEFAULT_USER.name, Password("I'm not even trying to be a good password"))
        b.markDone("when I login with the wrong credentials,")

        assertEquals(LoginResult.FAILURE, status)
        b.markDone("then the system denies me access.")
    }

    @Test
    fun `if I enter an invalid password while registering, it will disallow it`() {
        val authPersistence = AuthenticationPersistence(PureMemoryDatabase())
        val au = AuthenticationUtilities(authPersistence)
        b.markDone("Given I am not registered,")

        val data = mapOf("username" to DEFAULT_USER.name.value, "password" to "too short", "employee" to "1")
        val ex = assertThrows(IllegalArgumentException::class.java){ RegisterAPI.handlePOST(au, data)}
        b.markDone("when I register with too short of a password,")

        assertEquals(passwordMustBeLargeEnoughMsg, ex.message)
        b.markDone("then the system denies the registration on the basis of a bad password.")
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
