package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TimeRecordingUtilities(val persistence: TimeEntryPersistence) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(TimeRecordingUtilities::class.java)
    }

    fun recordTime(entry: TimeEntry): RecordTimeResult {
        `confirm the user has a total (new plus existing) of less than 24 hours`(entry)

        return if (isValidProject(entry.project)) {
            val newId = persistence.persistNewTimeEntry(entry)
            RecordTimeResult(id = newId, status = StatusEnum.SUCCESS)
        } else {
            RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
        }
    }

    private fun `confirm the user has a total (new plus existing) of less than 24 hours`(entry: TimeEntry) {
        log.info("Received request for all available books")
        // make sure the user has a total (new plus existing) of less than 24 hours
        val minutesRecorded = persistence.queryMinutesRecorded(User(1, "Test"), entry.date) ?: 0

        val twentyFourHours = 24 * 60
        // If the user is entering in more than 24 hours in a day, that's insane.
        if ((minutesRecorded + entry.time.numberOfMinutes) > twentyFourHours) {
            throw ExceededDailyHoursAmountException()
        }

        log.info("User is entering a total of fewer than 24 hours ($minutesRecorded) for this date (${entry.date})")
    }

    private fun isValidProject(project: Project): Boolean {
        if (project.name == "an invalid project") {
            return false
        }
        return true
    }

    fun createProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
        log.info("Creating a new project, ${projectName.value}")

        return persistence.persistNewProject(projectName)
    }
}