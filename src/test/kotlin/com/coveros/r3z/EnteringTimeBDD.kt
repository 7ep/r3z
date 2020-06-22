package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import com.coveros.r3z.timerecording.TimeEntryPersistence
import com.coveros.r3z.timerecording.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

/**
 * As an employee
 * I want to record my time
 * So that I am easily able to document my time in an organized way
 */
class EnteringTimeBDD {

    /**
     * Just a happy path for entering a time entry
     */
    @Test
    fun `capability to enter time`() {
        // `given I have worked 1 hour on project "A" on Monday`()
        val entry = createTimeEntry()
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)
        val timeRecordingUtilities = createTimeRecordingUtility()

        // `when I enter in that time`()
        val recordStatus = timeRecordingUtilities.recordTime(entry)

        // `then the system indicates it has persisted the new information`()
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
    }

    /**
     * A test helper method to generate a [TimeRecordingUtilities]
     * with a real database connected - H2
     */
    private fun createTimeRecordingUtility(): TimeRecordingUtilities {
        val dbAccessHelper = initializeDatabaseForTest()
        val timeEntryPersistence = TimeEntryPersistence(dbAccessHelper)
        return TimeRecordingUtilities(timeEntryPersistence)
    }

    private fun initializeDatabaseForTest() : IDbAccessHelper {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper: IDbAccessHelper =
                DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
        return dbAccessHelper
    }


    /**
     * Just another flavor of happy path
     */
    @Test
    @Ignore
    fun `A user enters six hours on a project with copious notes`() {
        // `given I have worked 6 hour on project "A" on Monday with a lot of notes`()
        val entry = createTimeEntry(
            time = Time(60 * 6),
            details = Details("Four score and seven years ago, blah blah blah".repeat(10)))
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)
        val timeRecordingUtilities = createTimeRecordingUtility()

        // `when I enter in that time`()
        val recordStatus = timeRecordingUtilities.recordTime(entry)

        // `then the system indicates it has persisted the new information`()
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
    }


    @Test
    @Ignore
    fun `If a user enters 0 hours on a project do nothing, but don't complain`() {
        // given I did some work but accidentally typed 0 for hours
        val entry = createTimeEntry(time=Time(0))
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)
        val timeRecordingUtilities = createTimeRecordingUtility()

        // `when I enter in that time`()
        val recordStatus = timeRecordingUtilities.recordTime(entry)

        // then the system reacts as though I entered nothing
        assertEquals("the system reacts as though I entered nothing", expectedStatus, recordStatus)
    }

    @Test
    fun `A user has already entered 24 hours for the day, they cannot enter more time on a new entry`() {
        // given the user has already entered 24 hours of time entries before

        // when they enter in a new time entry for one hour

        // then the system disallows it
    }

    @Test
    fun `A user cannot enter more than 24 hours in a single day`() {
        // given someone has somehow done 25 hours of work in a single day

        // when they try entering that...

        // the system disallows it
    }

    @Test
    fun `cannot enter time if you're an invalid user`() {
        // given you were banned from entering hours for whatever reason

        // when you try to enter time

        // the system disallows it

    }


    @Test
    fun `cannot enter time if your project is invalid`() {
        // given a project is banned from entering hours for whatever reason

        // when you try to enter time on that project

        // the system disallows it
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