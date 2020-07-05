package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TimeRecordingUtilities(private val persistence: TimeEntryPersistence) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(TimeRecordingUtilities::class.java)
    }

    fun recordTime(entry: TimeEntry): RecordTimeResult {
        log.info("Starting to record time for $entry")
        `confirm the user has a total (new plus existing) of less than 24 hours`(entry)
        try {
            val newId = persistence.persistNewTimeEntry(entry)
            assert(newId > 0) {"the new id must be positive at this point"}
            log.info("recorded time sucessfully")
            return RecordTimeResult(id = newId, status = StatusEnum.SUCCESS)
        } catch (ex : org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException) {
            log.info("time was not recorded successfully: error was: ${ex.message}")
            val indicatesProjectConstraintFailure = "REFERENCES TIMEANDEXPENSES.PROJECT(ID)"
            if (ex.message!!.contains(indicatesProjectConstraintFailure)) {
                return RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
            } else {
                // at this point, we're in unknown territory, may as well throw
                throw ex
            }
        }
    }

    private fun `confirm the user has a total (new plus existing) of less than 24 hours`(entry: TimeEntry) {
        log.info("checking that the user has a total (new plus existing) of less than 24 hours")
        // make sure the user has a total (new plus existing) of less than 24 hours
        val minutesRecorded = persistence.queryMinutesRecorded(User(1, "Test"), entry.date) ?: 0

        val twentyFourHours = 24 * 60
        // If the user is entering in more than 24 hours in a day, that's invalid.
        if ((minutesRecorded + entry.time.numberOfMinutes) > twentyFourHours) {
            log.info("User entered more time than exists in a day")
            throw ExceededDailyHoursAmountException()
        }

        log.info("User is entering a total of fewer than 24 hours ($minutesRecorded) for this date (${entry.date})")
    }

    fun createProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
        log.info("Creating a new project, ${projectName.value}")

        return persistence.persistNewProject(projectName)
    }
}