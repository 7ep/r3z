package coverosR3z.authentication.utility

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.system.misc.*
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File

class SessionTests {

    /**
     * If a user logs out, we should find all their sessions
     * in the sessions table and wipe them out
     *
     * Also test this is getting persisted to disk.  To do that,
     * we will check that we got what we expected, then reload
     * the database and confirm we get the same results
     */
    @IntegrationTest(usesDirectory = true)
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldClearAllSessionsWhenLogout() {
        val dbDirectory = DEFAULT_DB_DIRECTORY + "testShouldClearAllSessionsWhenLogout/"
        File(dbDirectory).deleteRecursively()
        val pmd = DatabaseDiskPersistence(dbDirectory, testLogger).startWithDiskPersistence()
        val authPersistence = AuthenticationPersistence(pmd, testLogger)
        val au = AuthenticationUtilities(pmd, testLogger, CurrentUser(SYSTEM_USER))
        val cu = CurrentUser(SYSTEM_USER)
        val tru = TimeRecordingUtilities(pmd, cu, testLogger)

        val employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)

        // we have to register users so reloading the data from disk works
        val (_, user1) = au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, employee)
        val (_, user2) = au.registerWithEmployee(DEFAULT_USER_2.name, DEFAULT_PASSWORD, employee)

        au.createNewSession(user1, DEFAULT_DATETIME) { "abc" }
        au.createNewSession(user1, DEFAULT_DATETIME) { "def" }
        au.createNewSession(user2, DEFAULT_DATETIME) { "ghi" }

        // wipe out all the sessions for this user
        au.logout(user1)

        // check that user1 lacks sessions and user2 still has theirs
        Assert.assertTrue(authPersistence.getAllSessions().none { it.user == user1 })
        Assert.assertEquals(1, authPersistence.getAllSessions().filter { it.user == user2 }.size)
        pmd.stop()

        // test out loading it from the disk
        val pmd2 = DatabaseDiskPersistence(
            dbDirectory = dbDirectory,
            logger = testLogger,
        ).startWithDiskPersistence()
        val authPersistence2 = AuthenticationPersistence(pmd2, testLogger)
        Assert.assertTrue(authPersistence2.getAllSessions().none { it.user == user1 })
        Assert.assertEquals(1, authPersistence2.getAllSessions().filter { it.user == user2 }.size)
    }
}