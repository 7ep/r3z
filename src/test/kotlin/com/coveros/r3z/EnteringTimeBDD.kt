package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
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
    @Ignore
    fun `capability to enter time`() {
        // `given I have worked 1 hour on project "A" on Monday`()
        val user = User(1, "I")
        val time = Time(60)
        val project = Project(1, "A")
        val entry = TimeEntry(user, project, time)
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)

        // `when I enter in that time`()
        val pool = getMemoryBasedDatabaseConnectionPool()
        val timeEntryPersistence = TimeEntryPersistence(pool)
        val timeRecordingUtilities = TimeRecordingUtilities(timeEntryPersistence)
        val recordStatus : RecordTimeResult = timeRecordingUtilities.recordTime(entry)

        // `then the system indicates it has persisted the new information`()
        assertEquals("We should see success", expectedStatus, recordStatus)
    }


    /**
     * Just another flavor of happy path
     */
    @Test
    @Ignore
    fun `A user enters six hours on a project with copious notes`() {
        // `given I have worked 6 hour on project "A" on Monday with a lot of notes`()
        val user = User(1, "I")
        val time = Time(60 * 6)
        val project = Project(1, "A")
        val details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        val entry = TimeEntry(user, project, time, details)
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)

        // `when I enter in that time`()
        val pool = getMemoryBasedDatabaseConnectionPool()
        val timeEntryPersistence = TimeEntryPersistence(pool)
        val timeRecordingUtilities = TimeRecordingUtilities(timeEntryPersistence)
        val recordStatus : RecordTimeResult = timeRecordingUtilities.recordTime(entry)

        // `then the system indicates it has persisted the new information`()
        assertEquals("We should see success", expectedStatus, recordStatus)
    }


    @Test
    @Ignore
    fun `If a user enters 0 hours on a project do nothing, but don't complain`() {
        // given I did some work but accidentally typed 0 for hours
        val user = User(1, "I")
        val time = Time(0)
        val project = Project(1, "A")
        val details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        val entry = TimeEntry(user, project, time, details)
        val expectedStatus = RecordTimeResult(1, StatusEnum.SUCCESS)

        // when I enter that in
        val pool = getMemoryBasedDatabaseConnectionPool()
        val timeEntryPersistence = TimeEntryPersistence(pool)
        val timeRecordingUtilities = TimeRecordingUtilities(timeEntryPersistence)
        val recordStatus : RecordTimeResult = timeRecordingUtilities.recordTime(entry)

        // then the system reacts as though I entered nothing
        assertEquals("We should see success", expectedStatus, recordStatus)
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

}