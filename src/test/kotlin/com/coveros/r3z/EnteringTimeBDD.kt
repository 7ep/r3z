package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.FlywayHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import com.coveros.r3z.timerecording.TimeEntryPersistence
import com.coveros.r3z.timerecording.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

/**
 * Feature: Entering time
 *
 * User story:
 *      As an employee
 *      I want to record my time
 *      So that I am easily able to document my time in an organized way
 */
class EnteringTimeBDD {

    /**
     * Just a happy path for entering a time entry
     */
    @Test
    fun `capability to enter time`() {
        // given I have worked 1 hour on project "A" on Monday
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)
        val dbAccessHelper = initializeDatabaseForTest()
        val tru = createTimeRecordingUtility(dbAccessHelper)
        val newProject : Project = tru.createProject(ProjectName("A"))
        val entry = createTimeEntry(project = newProject)

        // when I enter in that time
        val recordStatus = tru.recordTime(entry)

        // then the system indicates it has persisted the new information
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
    }

    /**
     * Just another flavor of happy path
     */
    @Test
    fun `A user enters six hours on a project with copious notes`() {
        // given I have worked 6 hour on project "A" on Monday with a lot of notes
        val dbAccessHelper = initializeDatabaseForTest()
        val tru = createTimeRecordingUtility(dbAccessHelper)
        val newProject : Project = tru.createProject(ProjectName("A"))
        val entry = createTimeEntry(
            time = Time(60 * 6),
            project = newProject,
            details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        )
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)

        // when I enter in that time
        val recordStatus = tru.recordTime(entry)

        // then the system indicates it has persisted the new information
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
    }

    @Test
    fun `A user has already entered 24 hours for the day, they cannot enter more time on a new entry`() {
        // given the user has already entered 24 hours of time entries before
        val dbAccessHelper = initializeDatabaseForTest()
        val tru = createTimeRecordingUtility(dbAccessHelper)
        val newProject : Project = tru.createProject(ProjectName("A"))
        val existingTimeForTheDay = createTimeEntry(project = newProject, time = Time(60 * 24))
        tru.recordTime(existingTimeForTheDay)

        // when they enter in a new time entry for one hour
        val entry = createTimeEntry(time=Time(30), project=newProject)

        // then the system disallows it
        assertThrows(ExceededDailyHoursAmountException::class.java) { tru.recordTime(entry) }
    }

    @Test
    fun `A user cannot enter more than 24 hours in a single entry`() {
        // given someone has somehow done 25 hours of work in a single day
        // when they try entering that...
        // the system disallows it
        val ex = assertThrows(AssertionError::class.java) { createTimeEntry(time = Time((60 * 24) + 1)) }
        assertEquals(ex.message, "Entries do not span multiple days, thus must be <=24 hrs")
    }

    @Test
    fun `Performance test`() {
        // given I have worked on a project
        val dbAccessHelper = initializeDatabaseForTest()
        val startAfterDatabase = System.currentTimeMillis()
        val tru = createTimeRecordingUtility(dbAccessHelper)
        val newProject : Project = tru.createProject(ProjectName("A"))

        // when I enter in that time
        val numberOfSamples = 60 * 8
        val durations = LongArray(numberOfSamples)
        for (i in 1..numberOfSamples) {
            val start = System.currentTimeMillis()
            val entry = createTimeEntry(
                    time = Time(1),
                    project = newProject,
                    details = Details("Four score and seven years ago, blah blah blah"))
            tru.recordTime(entry)
            val finish = System.currentTimeMillis()
            val timeElapsed = finish - start
            durations[i-1] = timeElapsed
        }

        // the system should perform quickly
        val average = durations.average()
        val takesLessThanMillisecondsAverage = average < 3
        println("Durations:")
        durations.forEach { d -> print(" $d") }
        val endOfTest = System.currentTimeMillis()
        val totalTime = endOfTest - startAfterDatabase
        println("\nThe functions took a total of $totalTime milliseconds for $numberOfSamples database calls")

        assertTrue("the system should respond quickly, but with $numberOfSamples samples, this took $average", takesLessThanMillisecondsAverage)
    }

    /**
     * A test helper method to generate a [TimeRecordingUtilities]
     * with a real database connected - H2
     */
    private fun createTimeRecordingUtility(dbAccessHelper : IDbAccessHelper): TimeRecordingUtilities {
        val timeEntryPersistence = TimeEntryPersistence(dbAccessHelper)
        return TimeRecordingUtilities(timeEntryPersistence)
    }

    private fun initializeDatabaseForTest() : IDbAccessHelper {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper: IDbAccessHelper =
                DbAccessHelper(ds)
        val flywayHelper = FlywayHelper(ds)
        flywayHelper.cleanDatabase()
        flywayHelper.migrateDatabase()
        return dbAccessHelper
    }
}