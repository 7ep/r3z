package coverosR3z.timerecording

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.exceptions.EmployeeNotRegisteredException
import coverosR3z.logging.logInfo
import coverosR3z.persistence.ProjectIntegrityViolationException
import coverosR3z.persistence.EmployeeIntegrityViolationException

class TimeRecordingUtilities(private val persistence: ITimeEntryPersistence) {

    fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult {
        logInfo("Starting to record time for $entry")
        `confirm the employee has a total (new plus existing) of less than 24 hours`(entry)
        try {
            persistence.persistNewTimeEntry(entry)
            logInfo("recorded time sucessfully")
            return RecordTimeResult(id = null, status = StatusEnum.SUCCESS)
        } catch (ex : ProjectIntegrityViolationException) {
            logInfo("time was not recorded successfully: project id did not match a valid project")
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)
        } catch (ex : EmployeeIntegrityViolationException) {
            logInfo("time was not recorded successfully: employee id did not match a valid employee")
            return RecordTimeResult(id = null, status = StatusEnum.INVALID_EMPLOYEE)
        }
    }

    private fun `confirm the employee has a total (new plus existing) of less than 24 hours`(entry: TimeEntryPreDatabase) {
        logInfo("checking that the employee has a total (new plus existing) of less than 24 hours")
        // make sure the employee has a total (new plus existing) of less than 24 hours
        var minutesRecorded : Int
        try {
            minutesRecorded = persistence.queryMinutesRecorded(entry.employee, entry.date)
        } catch (ex : EmployeeNotRegisteredException) {
            // if we hit here, it means the employee doesn't exist yet.  For these purposes, that is
            // fine, we are just checking here that if a employee *does* exist, they don't have too many minutes.
            // if they don't exist, just move on through.
            logInfo("employee ${entry.employee} was not registered in the database.  returning 0 minutes recorded.")
            minutesRecorded = 0
        }

        val twentyFourHours = 24 * 60
        // If the employee is entering in more than 24 hours in a day, that's invalid.
        val existingPlusNewMinutes = minutesRecorded + entry.time.numberOfMinutes
        if (existingPlusNewMinutes > twentyFourHours) {
            logInfo("Employee entered more time than exists in a day: $existingPlusNewMinutes minutes")
            throw ExceededDailyHoursAmountException()
        }

        logInfo("Employee is entering a total of fewer than 24 hours ($existingPlusNewMinutes minutes / ${existingPlusNewMinutes / 60} hours) for this date (${entry.date})")
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
     * Business code for creating a new employee in the
     * system (persists it to the database)
     */
    fun createEmployee(employeename: EmployeeName) : Employee {
        assert(employeename.value.isNotEmpty()) {"Employee name cannot be empty"}
        logInfo("Creating a new employee, ${employeename.value}")

        return persistence.persistNewEmployee(employeename)
    }

    fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): List<TimeEntry> {
        return persistence.readTimeEntriesOnDate(employee, date)
    }

    fun getAllEntriesForEmployee(employee: Employee): List<TimeEntry> {
        return persistence.readTimeEntries(employee)
    }
}