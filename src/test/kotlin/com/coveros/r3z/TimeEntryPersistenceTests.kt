package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.timerecording.TimeEntryPersistence
import org.junit.Assert
import org.junit.Test

class TimeEntryPersistenceTests {

    @Test fun `can record a time entry to the database`() {
        val expectedNewId = 1
        val dbAccessHelper = DbAccessHelper(getMemoryBasedDatabaseConnectionPool())
        val tep = TimeEntryPersistence(dbAccessHelper)
        val result = tep.persistNewTimeEntry(createTimeEntry())

        Assert.assertEquals("we expect that the insertion of a new row will return the new id", expectedNewId, result)
    }

    /**
     * a test helper method to create a [TimeEntry]
     */
    private fun createTimeEntry(
            user : User = User(1, "I"),
            time : Time = Time(60),
            project : Project = Project(1, "A"),
            details : Details = Details()
    ): TimeEntry {
        return TimeEntry(user, project, time, details)
    }
}