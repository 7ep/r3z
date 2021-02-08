package coverosR3z.timerecording.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.logging.logAudit
import coverosR3z.logging.logDebug
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.exceptions.ExceededDailyHoursAmountException
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*

class TimeRecordingUtilities(private val persistence: ITimeEntryPersistence, private val cu : CurrentUser) :
    ITimeRecordingUtilities {

    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        return TimeRecordingUtilities(persistence, cu)
    }

    override fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult {
        return createOrModifyEntry(entry) {
            val newTimeEntry = persistence.persistNewTimeEntry(entry)
            logDebug(cu) {"recorded time successfully"}
            newTimeEntry
        }
    }

    override fun changeEntry(entry: TimeEntry): RecordTimeResult{
        return createOrModifyEntry(entry.toTimeEntryPreDatabase()) {
            val newTimeEntry = persistence.overwriteTimeEntry(entry)
            logDebug(cu) {"modified time successfully"}
            newTimeEntry
        }
    }

    private fun createOrModifyEntry(entry: TimeEntryPreDatabase, behavior: () -> TimeEntry): RecordTimeResult{
        val user = cu.user
        // ensure time entry user is the logged in user, or
        // is the system
        if (user != SYSTEM_USER && user.employeeId != entry.employee.id) {
            logAudit(cu) {"time was not recorded successfully: current user ${user.name.value} does not have access " +
                    "to modify time for ${entry.employee.name.value}"}
            return RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null)
        }
        logAudit(cu) {"Recording ${entry.time.numberOfMinutes} minutes on \"${entry.project.name.value}\""}
        confirmLessThan24Hours(entry.time, entry.employee, entry.date)
        if(persistence.isInASubmittedPeriod(entry.employee.id, entry.date)){
            return RecordTimeResult(StatusEnum.LOCKED_ALREADY_SUBMITTED)
        }
        return try {
            val newTimeEntry = behavior()
            RecordTimeResult(StatusEnum.SUCCESS, newTimeEntry)
        } catch (ex : IllegalStateException) {
            logDebug(cu) {"Error adding time entry: ${ex.message}"}

            when (ex.message) {
                TimeEntryPersistence.timeEntryInvalidBadEmployee -> RecordTimeResult(StatusEnum.INVALID_EMPLOYEE, null)
                TimeEntryPersistence.timeEntryInvalidBadProject -> RecordTimeResult(StatusEnum.INVALID_PROJECT, null)
                TimeEntryPersistence.timeEntryInvalidNoEmployee -> RecordTimeResult(StatusEnum.INVALID_EMPLOYEE, null)
                TimeEntryPersistence.timeEntryInvalidNoProject -> RecordTimeResult(StatusEnum.INVALID_PROJECT, null)
                else -> RecordTimeResult(StatusEnum.NULL, null)
            }
        }
    }

    private fun confirmLessThan24Hours(time: Time, employee: Employee, date: Date) {
        logDebug(cu) {"confirming total time is less than 24 hours"}
        // make sure the employee has a total (new plus existing) of less than 24 hours
        val minutesRecorded : Time = persistence.queryMinutesRecorded(employee, date)
        val twentyFourHours = 24 * 60
        // If the employee is entering in more than 24 hours in a day, that's invalid.
        val existingPlusNewMinutes = minutesRecorded.numberOfMinutes + time.numberOfMinutes
        if (existingPlusNewMinutes > twentyFourHours) {
            logDebug(cu) {"More minutes entered ($existingPlusNewMinutes) than exists in a day (1440)"}
            throw ExceededDailyHoursAmountException()
        }
    }

    /**
     * Business code for creating a new project in the
     * system (persists it to the database)
     */
    /**
     * Business code for creating a new project in the
     * system (persists it to the database)
     */
    override fun createProject(projectName: ProjectName) : Project {
        require(persistence.getProjectByName(projectName) == NO_PROJECT) {"Cannot create a new project if one already exists by that same name"}
        logAudit(cu) {"Creating a new project, \"${projectName.value}\""}

        return persistence.persistNewProject(projectName)
    }

    /**
     * Business code for creating a new employee in the
     * system (persists it to the database)
     */
    /**
     * Business code for creating a new employee in the
     * system (persists it to the database)
     */
    override fun createEmployee(employeename: EmployeeName) : Employee {
        require(employeename.value.isNotEmpty()) {"Employee name cannot be empty"}

        val newEmployee = persistence.persistNewEmployee(employeename)
        logAudit(cu) {"Creating a new employee, \"${newEmployee.name.value}\""}
        return newEmployee
    }

    override fun getEntriesForEmployeeOnDate(employeeId: EmployeeId, date: Date): Set<TimeEntry> {
        val employee = persistence.getEmployeeById(employeeId)
        return persistence.readTimeEntriesOnDate(employee, date)
    }

    override fun getAllEntriesForEmployee(employeeId: EmployeeId): Set<TimeEntry> {
        val employee = persistence.getEmployeeById(employeeId)
        return persistence.readTimeEntries(employee)
    }

    override fun listAllProjects(): List<Project> {
        return persistence.getAllProjects()
    }

    override fun findProjectById(id: ProjectId): Project {
        return persistence.getProjectById(id)
    }

    override fun findEmployeeById(id: EmployeeId): Employee {
        return persistence.getEmployeeById(id)
    }

    override fun listAllEmployees(): List<Employee> {
        return persistence.getAllEmployees()
    }

    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        return persistence.persistNewSubmittedTimePeriod(checkNotNull(cu.user.employeeId), timePeriod)
    }

}