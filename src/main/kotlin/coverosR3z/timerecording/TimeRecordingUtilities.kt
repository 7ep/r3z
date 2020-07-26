package coverosR3z.timerecording

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.exceptions.UserNotRegisteredException
import coverosR3z.logging.logInfo
import coverosR3z.persistence.ProjectIntegrityViolationException
import coverosR3z.persistence.UserIntegrityViolationException

class TimeRecordingUtilities(private val persistence: ITimeEntryPersistence) {

    fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult {
        logInfo("Starting to record time for $entry")
        `confirm the user has a total (new plus existing) of less than 24 hours`(entry)
        try {
            persistence.persistNewTimeEntry(entry)
            logInfo("recorded time sucessfully")
            return RecordTimeResult(id = null, status = StatusEnum.SUCCESS)
        } catch (ex : ProjectIntegrityViolationException) {
            logInfo("time was not recorded successfully: project id did not match a valid project")
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
        } catch (ex : UserIntegrityViolationException) {
            logInfo("time was not recorded successfully: user id did not match a valid user")
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_USER)
        }
    }

    private fun `confirm the user has a total (new plus existing) of less than 24 hours`(entry: TimeEntryPreDatabase) {
        logInfo("checking that the user has a total (new plus existing) of less than 24 hours")
        // make sure the user has a total (new plus existing) of less than 24 hours
        var minutesRecorded : Int
        try {
            minutesRecorded = persistence.queryMinutesRecorded(entry.user, entry.date)
        } catch (ex : UserNotRegisteredException) {
            // if we hit here, it means the user doesn't exist yet.  For these purposes, that is
            // fine, we are just checking here that if a user *does* exist, they don't have too many minutes.
            // if they don't exist, just move on through.
            logInfo("user ${entry.user} was not registered in the database.  returning 0 minutes recorded.")
            minutesRecorded = 0
        }

        val twentyFourHours = 24 * 60
        // If the user is entering in more than 24 hours in a day, that's invalid.
        val existingPlusNewMinutes = minutesRecorded + entry.time.numberOfMinutes
        if (existingPlusNewMinutes > twentyFourHours) {
            logInfo("User entered more time than exists in a day: $existingPlusNewMinutes minutes")
            throw ExceededDailyHoursAmountException()
        }

        logInfo("User is entering a total of fewer than 24 hours ($existingPlusNewMinutes minutes / ${existingPlusNewMinutes / 60} hours) for this date (${entry.date})")
    }

    /**
     * Business code for creating a new project in the
     * system (persists it to the database)
     */
    fun createProject(projectName: ProjectName) : Project {
        assert(projectName.value.isNotEmpty()) {"Project name cannot be empty"}
        logInfo("Creating a new project, ${projectName.value}")

        return persistence.persistNewProject(projectName)
    }

    /**
     * Business code for creating a new user in the
     * system (persists it to the database)
     */
    fun createUser(username: UserName) : User {
        assert(username.value.isNotEmpty()) {"User name cannot be empty"}
        logInfo("Creating a new user, ${username.value}")

        return persistence.persistNewUser(username)
    }

    fun getEntriesForUserOnDate(user: User, date: Date): List<TimeEntry> {
        return persistence.readTimeEntriesOnDate(user, date)
    }

    fun getAllEntriesForUser(user: User): List<TimeEntry> {
        return persistence.readTimeEntries(user)
    }
}