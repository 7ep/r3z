package coverosR3z.authentication

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.Invitation
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.system.misc.*
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import coverosR3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Category(IntegrationTestCategory::class)
class AuthenticationPersistenceTests {

    lateinit var ap: AuthenticationPersistence
    lateinit var pmd: PureMemoryDatabase

    @Before
    fun init() {
        pmd = createEmptyDatabase()
        ap = AuthenticationPersistence(pmd, testLogger)
    }

    @Test
    fun `Should fail to find an unregistered user`() {
        val result = ap.isUserRegistered(UserName("mitch"))

        assertEquals("we haven't registered anyone yet, so mitch shouldn't be registered", false, result)
    }

    @Test
    fun `Should be able to create a new user`() {
        ap.createUser(UserName("jenna"), DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE, DEFAULT_USER.role)

        assertTrue(ap.isUserRegistered(UserName("jenna")))
    }

    /**
     * If a user successfully authenticates, we should create a session entry,
     */
    @Test
    fun testShouldAddSession() {
        ap.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to add a session for a user when one already exists, throw exception
     */
    @Test
    fun testShouldAddSession_Duplicate() {
        ap.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        val ex = Assert.assertThrows(IllegalArgumentException::class.java) {
            ap.addNewSession(
                DEFAULT_SESSION_TOKEN,
                DEFAULT_USER,
                DEFAULT_DATETIME
            )
        }
        assertEquals("There must not already exist a session for (${DEFAULT_USER.name}) if we are to create one", ex.message)
    }

    /**
     * When a user is no longer authenticated, we enact that
     * by removing their entry from the sessions.
     */
    @Test
    fun testShouldRemoveSession() {
        ap.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
        ap.deleteSession(DEFAULT_USER)
        assertEquals(NO_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to remove a session but it doesn't exist, throw an exception
     */
    @Test
    fun testShouldComplainIfTryingToRemoveNonexistentSession() {
        val ex = Assert.assertThrows(IllegalStateException::class.java) { ap.deleteSession(DEFAULT_USER) }
        assertEquals("There must exist a session in the database for (${DEFAULT_USER.name.value}) in order to delete it", ex.message)
    }


    /**
     * See [coverosR3z.persistence.PureMemoryDatabaseTests.testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingSessionDataWithMultiThreading() {
        val listOfThreads = mutableListOf<Future<*>>()
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        val numberNewSessionsAdded = 20
        for(i in 1..numberNewSessionsAdded) { // each thread calls the add a single time
            listOfThreads.add(cachedThreadPool.submit(Thread {
                ap.addNewSession(DEFAULT_SESSION_TOKEN +i, DEFAULT_USER, DEFAULT_DATETIME)
            }))
        }
        // wait for all those threads
        listOfThreads.forEach{it.get()}
        assertEquals(numberNewSessionsAdded, ap.getAllSessions().size)
    }


    /**
     * See [coverosR3z.persistence.PureMemoryDatabaseTests.testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingUserDataWithMultiThreading() {
        val listOfThreads = mutableListOf<Future<*>>()
        val numberNewUsersAdded = 20
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        repeat(numberNewUsersAdded) { // each thread calls the add a single time
            listOfThreads.add(cachedThreadPool.submit(Thread {
                ap.createUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE, DEFAULT_USER.role)
            }))
        }
        // wait for all those threads
        listOfThreads.forEach{it.get()}
        assertEquals(numberNewUsersAdded, ap.getAllUsers().size)
    }

    @Test
    fun testAddRoleToUser() {
        val expected = DEFAULT_USER.copy(role = Role.ADMIN)
        ap.createUser(DEFAULT_USER.name, DEFAULT_USER.hash, DEFAULT_USER.salt, DEFAULT_USER.employee, DEFAULT_USER.role)
        val result = ap.addRoleToUser(DEFAULT_USER, Role.ADMIN)
        assertEquals(expected.role, result.role)
    }

    @Test
    fun testCanAddInvitation() {
        val result = ap.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME, DEFAULT_INVITATION_CODE)

        assertTrue(pmd.dataAccess<Invitation>(Invitation.directoryName).read {
            invitations -> invitations.all { it == result }
        })
    }

    @Test
    fun testCanRemoveInvitation() {
        ap.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME, DEFAULT_INVITATION_CODE)
        ap.removeInvitation(DEFAULT_EMPLOYEE)

        assertTrue(pmd.dataAccess<Invitation>(Invitation.directoryName).read {
                invitations -> invitations.count() == 0
        })
    }

    @Test
    fun testCanGetInvitationByEmployee() {
        val result = ap.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME, DEFAULT_INVITATION_CODE)
        val employee = ap.getEmployeeFromInvitationCode(result.code)

        assertEquals(DEFAULT_EMPLOYEE, employee)
    }

    /**
     * happy path - find a valid user in the database by employee
     */
    @Test
    fun testCanGetUserByEmployee() {
        val newUser = ap.createUser(DEFAULT_USER.name, DEFAULT_USER.hash, DEFAULT_USER.salt, DEFAULT_USER.employee, DEFAULT_USER.role)
        val result = ap.getUserByEmployee(DEFAULT_USER.employee)
        assertEquals(result, newUser)
    }

    /**
     * If the user wasn't found in the database with
     * this employee value
     */
    @Test
    fun testCanGetUserByEmployee_NoUserFound() {
        val result = ap.getUserByEmployee(DEFAULT_USER.employee)
        assertEquals(result, NO_USER)
    }

    /**
     * Similar to [testCanGetUserByEmployee_NoUserFound] but
     * we're passing in [NO_EMPLOYEE]
     */
    @Test
    fun testCanGetUserByEmployee_NoUserFound_NO_EMPLOYEE() {
        val result = ap.getUserByEmployee(NO_EMPLOYEE)
        assertEquals(result, NO_USER)
    }

}