package coverosR3z.authentication

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Roles
import coverosR3z.authentication.types.UserName
import coverosR3z.misc.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class AuthenticationPersistenceTests {

    @Test
    fun `Should fail to find an unregistered user`() {
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        val result = ap.isUserRegistered(UserName("mitch"))

        assertEquals("we haven't registered anyone yet, so mitch shouldn't be registered", false, result)
    }

    @Test
    fun `Should be able to create a new user`() {
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        ap.createUser(UserName("jenna"), DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE.id)

        assertTrue(ap.isUserRegistered(UserName("jenna")))
    }

    /**
     * If a user successfully authenticates, we should create a session entry,
     */
    @Test
    fun testShouldAddSession() {
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        ap.addNewSession(DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)
        assertEquals(DEFAULT_USER, ap.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * If we try to add a session for a user when one already exists, throw exception
     */
    @Test
    fun testShouldAddSession_Duplicate() {
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
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
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
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
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        val ex = Assert.assertThrows(IllegalStateException::class.java) { ap.deleteSession(DEFAULT_USER) }
        assertEquals("There must exist a session in the database for (${DEFAULT_USER.name.value}) in order to delete it", ex.message)
    }


    /**
     * See [coverosR3z.persistence.PureMemoryDatabaseTests.testCorruptingEmployeeDataWithMultiThreading]
     */
    @Test
    fun testCorruptingSessionDataWithMultiThreading() {
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
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
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        val listOfThreads = mutableListOf<Future<*>>()
        val numberNewUsersAdded = 20
        val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
        repeat(numberNewUsersAdded) { // each thread calls the add a single time
            listOfThreads.add(cachedThreadPool.submit(Thread {
                ap.createUser(DEFAULT_USER.name, DEFAULT_HASH, DEFAULT_SALT, DEFAULT_EMPLOYEE.id)
            }))
        }
        // wait for all those threads
        listOfThreads.forEach{it.get()}
        assertEquals(numberNewUsersAdded, ap.getAllUsers().size)
    }

    @Test
    fun testAddRoleToUser() {
        val expected = DEFAULT_USER.copy(role = Roles.ADMIN)
        val ap = AuthenticationPersistence(PureMemoryDatabase(), testLogger)
        ap.createUser(DEFAULT_USER.name, DEFAULT_USER.hash, DEFAULT_USER.salt, DEFAULT_USER.employeeId)
        val result = ap.addRoleToUser(DEFAULT_USER.name, Roles.ADMIN)
        assertEquals(expected.role, result.role)
    }

}