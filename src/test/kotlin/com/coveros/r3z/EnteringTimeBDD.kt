package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import com.coveros.r3z.logging.Logger
import com.coveros.r3z.timerecording.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test
import com.coveros.r3z.persistence.PureMemoryDatabase
import com.coveros.r3z.timerecording.ITimeEntryPersistence
import com.coveros.r3z.timerecording.TimeEntryPersistence2


/**
 * Feature: Entering time
 *
 * User story:
 *      As an employee
 *      I want to record my time
 *      So that I am easily able to document my time in an organized way
 */
class EnteringTimeBDD {

    companion object {
        val log : Logger = Logger()
    }

    /**
     * Just a happy path for entering a time entry
     */
    @Test
    fun `capability to enter time`() {
        val (expectedStatus, tru, entry) = `given I have worked 1 hour on project "A" on Monday`()

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
        val (tru, entry, expectedStatus) = `given I have worked 6 hours on project "A" on Monday with a lot of notes`()

        // when I enter in that time
        val recordStatus = tru.recordTime(entry)

        // then the system indicates it has persisted the new information
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
    }

    @Test
    fun `A user has already entered 24 hours for the day, they cannot enter more time on a new entry`() {
        val (tru, newProject: Project) = `given the user has already entered 24 hours of time entries before`()

        // when they enter in a new time entry for one hour
        val entry = createTimeEntry(time=Time(30), project=newProject)

        // then the system disallows it
        assertThrows(ExceededDailyHoursAmountException::class.java) { tru.recordTime(entry) }
    }

    @Test
    fun `Performance test`() {
        // given I have worked on a project
        val startAfterDatabase = System.currentTimeMillis()
        val tru = createTimeRecordingUtility()
        val newProject : Project = tru.createProject(ProjectName("A"))

        // when I enter in that time
        val numberOfSamples = 10
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
        val maximumAllowableMilliseconds = 5
        val takesLessThanMillisecondsAverage = average < maximumAllowableMilliseconds
        log.info("Durations:")
        durations.forEach { d -> print(" $d") }
        val endOfTest = System.currentTimeMillis()
        val totalTime = endOfTest - startAfterDatabase
        log.info("\nThe functions took a total of $totalTime milliseconds for $numberOfSamples database calls")

        assertTrue("average should be less than $maximumAllowableMilliseconds milliseconds, but with $numberOfSamples samples, this took $average", takesLessThanMillisecondsAverage)
    }

    private fun `given I have worked 1 hour on project "A" on Monday`(): Triple<RecordTimeResult, TimeRecordingUtilities, TimeEntry> {
        val expectedStatus = RecordTimeResult(null, StatusEnum.SUCCESS)
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(ProjectName("A"))
        val entry = createTimeEntry(project = newProject)
        return Triple(expectedStatus, tru, entry)
    }

    private fun `given I have worked 6 hours on project "A" on Monday with a lot of notes`(): Triple<TimeRecordingUtilities, TimeEntry, RecordTimeResult> {
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(ProjectName("A"))
        val entry = createTimeEntry(
                time = Time(60 * 6),
                project = newProject,
                details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        )
        val expectedStatus = RecordTimeResult(null, StatusEnum.SUCCESS)
        return Triple(tru, entry, expectedStatus)
    }

    private fun `given the user has already entered 24 hours of time entries before`(): Pair<TimeRecordingUtilities, Project> {
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(ProjectName("A"))
        val existingTimeForTheDay = createTimeEntry(project = newProject, time = Time(60 * 24))
        tru.recordTime(existingTimeForTheDay)
        return Pair(tru, newProject)
    }

    /**
     * A test helper method to generate a [TimeRecordingUtilities]
     * with a real database connected - H2
     */
    private fun createTimeRecordingUtility(): TimeRecordingUtilities {
        val timeEntryPersistence : ITimeEntryPersistence = TimeEntryPersistence2(PureMemoryDatabase())
        return TimeRecordingUtilities(timeEntryPersistence)
    }

}