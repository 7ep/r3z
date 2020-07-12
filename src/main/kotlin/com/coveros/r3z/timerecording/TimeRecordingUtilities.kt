package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import com.coveros.r3z.logging.Logger
import com.coveros.r3z.persistence.ProjectIntegrityViolationException
import com.coveros.r3z.persistence.UserIntegrityViolationException

class TimeRecordingUtilities(val persistence: ITimeEntryPersistence) {

    companion object {
        val log : Logger = Logger()
    }

    fun recordTime(entry: TimeEntry): RecordTimeResult {
        log.info("Starting to record time for $entry")
        `confirm the user has a total (new plus existing) of less than 24 hours`(entry)
        try {
            persistence.persistNewTimeEntry(entry)
            log.info("recorded time sucessfully")
            return RecordTimeResult(id = null, status = StatusEnum.SUCCESS)
        } catch (ex : ProjectIntegrityViolationException) {
            log.info("time was not recorded successfully: project id did not match a valid project")
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
        } catch (ex : UserIntegrityViolationException) {
            log.info("time was not recorded successfully: user id did not match a valid user")
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_USER)
        }
    }

    private fun `confirm the user has a total (new plus existing) of less than 24 hours`(entry: TimeEntry) {
        log.info("checking that the user has a total (new plus existing) of less than 24 hours")
        // make sure the user has a total (new plus existing) of less than 24 hours
        val minutesRecorded = persistence.queryMinutesRecorded(entry.user, entry.date)

        val twentyFourHours = 24 * 60
        // If the user is entering in more than 24 hours in a day, that's invalid.
        val existingPlusNewMinutes = minutesRecorded + entry.time.numberOfMinutes
        if (existingPlusNewMinutes > twentyFourHours) {
            log.info("User entered more time than exists in a day: $existingPlusNewMinutes minutes")
            throw ExceededDailyHoursAmountException()
        }

        log.info("User is entering a total of fewer than 24 hours ($existingPlusNewMinutes) for this date (${entry.date})")
    }

    /**
     * Business code for creating a new project in the
     * system (persists it to the database)
     */
    fun createProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
        log.info("Creating a new project, ${projectName.value}")

        return persistence.persistNewProject(projectName)
    }

    /**
     * Business code for creating a new user in the
     * system (persists it to the database)
     */
    fun createUser(username: UserName) : User {
        assert(username.value.isNotEmpty()) {"User name cannot be empty"}
        log.info("Creating a new user, ${username.value}")

        return persistence.persistNewUser(username)
    }
}