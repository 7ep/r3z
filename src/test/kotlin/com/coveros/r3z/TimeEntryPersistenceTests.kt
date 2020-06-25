package com.coveros.r3z

import com.coveros.r3z.domainobjects.Details
import com.coveros.r3z.domainobjects.ProjectName
import com.coveros.r3z.domainobjects.Time
import com.coveros.r3z.domainobjects.User
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import com.coveros.r3z.timerecording.TimeEntryPersistence
import org.h2.jdbc.JdbcSQLDataException
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.junit.Assert
import org.junit.Test
import java.sql.Date

class TimeEntryPersistenceTests {

    private val MAX_DETAIL_TEXT_LENGTH = 500

    @Test fun `can record a time entry to the database`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val expectedNewId : Long = 1
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val result = tep.persistNewTimeEntry(createTimeEntry(project = newProject))

        val message = "we expect that the insertion of a new row will return the new id"
        Assert.assertEquals(message, expectedNewId, result)
    }

    /**
     * If we try to add a time entry with a project id that doesn't exist in
     * the database, we should get an exception back from the database
     */
    @Test fun `Can't record a time entry that has a nonexistent project id`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        Assert.assertThrows(JdbcSQLIntegrityConstraintViolationException::class.java) {
            tep.persistNewTimeEntry(createTimeEntry())
        }
    }

    /**
     * Details only takes up to MAX_DETAIL_TEXT_LENGTH characters.  Any more and an exception will be thrown.
     */
    @Test fun `Can't record a time entry with too many letters in details`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))

        Assert.assertThrows(JdbcSQLDataException::class.java) {
            dbAccessHelper.executeInsert(
                    "Creates a new time entry in the database - a record of a particular users's time on a project",
                    "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, details) VALUES (?, ?, ?, ?);",
                    1, newProject.id, 60, "a".repeat(MAX_DETAIL_TEXT_LENGTH + 1))
        }
    }

    /**
     * Details only takes up to MAX_DETAIL_TEXT_LENGTH characters.  Any more and an exception will be thrown.
     * This checks the unicode version of that idea.
     */
    @Test fun `Can't record a time entry with too many letters in details, using unicode`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        // the following unicode is longer than it seems.  Thanks Matt!
        val unicodeWeirdCharacters = "h̬͕̘ͅiͅ ̘͔̝̺̩͚͚̕b̧͈̙͕̰̖̯y̡̺r̙o̦̯̙͎̮n̯̘̣͖"
        // we want to think about the MAX_DETAIL_TEXT_LENGTH character limit for the details varchar field,
        // how does it relate to unicode chars?  Here, we'll calculate the number of times
        // we think it should fit into the given maximum character length, plus one in
        // order to bust through the ceiling.

        val numberTimesToRepeat = MAX_DETAIL_TEXT_LENGTH/unicodeWeirdCharacters.length + 1
        // as of the time of writing, numberTimesToRepeat was 13
        print("only have to repeat $numberTimesToRepeat times for this to bust the ceiling")

        Assert.assertThrows(JdbcSQLDataException::class.java) {
            dbAccessHelper.executeInsert(
                    "Creates a new time entry in the database - a record of a particular users's time on a project",
                    "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, details) VALUES (?, ?, ?, ?);",
                    1, newProject.id, 60, unicodeWeirdCharacters.repeat(numberTimesToRepeat))
        }
    }

    /**
     * Make sure that when we use really weird unicode, it's accepted
     */
    @Test fun `Can record a time entry with unicode letters in details`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))

        val newId = dbAccessHelper.executeInsert(
                "Creates a new time entry in the database - a record of a particular users's time on a project",
                "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, details) VALUES (?, ?, ?, ?);",
                1, newProject.id, 60, "h̬͕̘ͅiͅ ̘͔̝̺̩͚͚̕b̧͈̙͕̰̖̯y̡̺r̙o̦̯̙͎̮n̯̘̣͖")
        Assert.assertEquals("we should get a new id for the new timeentry", 1, newId)
    }

    /**
     * Can we store data from non-English alphabets? you betcha
     */
    @Test fun `Can record a time entry with unicode chars`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val expectedNewId : Long = 1
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val result = tep.persistNewTimeEntry(createTimeEntry(project = newProject, details = Details(" Γεια σου κόσμε! こんにちは世界 世界，你好")))

        val message = "we expect that the insertion of a new row will return the new id"
        Assert.assertEquals(message, expectedNewId, result)
    }

    /**
     * We need to be able to know how many hours a user has worked for the purpose of validation
     */
    @Test
    fun `Can query hours worked by a user on a given day`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        tep.persistNewTimeEntry(createTimeEntry(user=User(1,"test"), time= Time(60)))

        val query = tep.queryMinutesRecorded(user=User(1,"Test"), date=Date(3))
        Assert.assertTrue(query == 60)
    }

    private fun initializeDatabaseForTest() : IDbAccessHelper {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper: IDbAccessHelper =
                DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
        return dbAccessHelper
    }
}