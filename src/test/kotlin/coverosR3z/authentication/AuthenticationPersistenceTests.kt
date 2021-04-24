package coverosR3z.authentication

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.Invitation
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.system.misc.*
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class AuthenticationPersistenceTests {

    @Category(IntegrationTestCategory::class)
    @Test
    fun `Should fail to find an unregistered user`() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
        val result = ap.isUserRegistered(UserName("mitch"))

        assertEquals("we haven't registered anyone yet, so mitch shouldn't be registered", false, result)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun `Should be able to create a new user`() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
        ap.createUser(UserName("jenna"), DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE, DEFAULT_USER.role)

        assertTrue(ap.isUserRegistered(UserName("jenna")))
    }

    /**
     * If a user successfully authenticates, we should create a session entry,
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldAddSession() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
        ap.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to add a session for a user when one already exists, throw exception
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldAddSession_Duplicate() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
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
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldRemoveSession() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
        ap.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
        ap.deleteSession(DEFAULT_USER)
        assertEquals(NO_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to remove a session but it doesn't exist, throw an exception
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldComplainIfTryingToRemoveNonexistentSession() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
        val ex = Assert.assertThrows(IllegalStateException::class.java) { ap.deleteSession(DEFAULT_USER) }
        assertEquals("There must exist a session in the database for (${DEFAULT_USER.name.value}) in order to delete it", ex.message)
    }


    /**
     * See [coverosR3z.persistence.PureMemoryDatabaseTests.testCorruptingEmployeeDataWithMultiThreading]
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testCorruptingSessionDataWithMultiThreading() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
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
    @Category(IntegrationTestCategory::class)
    @Test
    fun testCorruptingUserDataWithMultiThreading() {
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
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

    @Category(IntegrationTestCategory::class)
    @Test
    fun testAddRoleToUser() {
        val expected = DEFAULT_USER.copy(role = Role.ADMIN)
        val ap = AuthenticationPersistence(createEmptyDatabase(), testLogger)
        ap.createUser(DEFAULT_USER.name, DEFAULT_USER.hash, DEFAULT_USER.salt, DEFAULT_USER.employee, DEFAULT_USER.role)
        val result = ap.addRoleToUser(DEFAULT_USER, Role.ADMIN)
        assertEquals(expected.role, result.role)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testCanAddInvitation() {
        val pmd = createEmptyDatabase()
        val ap = AuthenticationPersistence(pmd, testLogger)

        val result = ap.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME, DEFAULT_INVITATION_CODE)

        assertTrue(pmd.dataAccess<Invitation>(Invitation.directoryName).read {
            invitations -> invitations.all { it == result }
        })
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testCanRemoveInvitation() {
        val pmd = createEmptyDatabase()
        val ap = AuthenticationPersistence(pmd, testLogger)

        ap.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME, DEFAULT_INVITATION_CODE)
        ap.removeInvitation(DEFAULT_EMPLOYEE)

        assertTrue(pmd.dataAccess<Invitation>(Invitation.directoryName).read {
                invitations -> invitations.count() == 0
        })
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testCanGetInvitationByEmployee() {
        val pmd = createEmptyDatabase()
        val ap = AuthenticationPersistence(pmd, testLogger)

        val result = ap.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME, DEFAULT_INVITATION_CODE)
        val employee = ap.getEmployeeFromInvitationCode(result.code)

        assertEquals(DEFAULT_EMPLOYEE, employee)
    }



}