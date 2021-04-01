package coverosR3z.timerecording.utility

import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.utility.IRolesChecker
import coverosR3z.logging.ILogger
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.exceptions.ExceededDailyHoursAmountException
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*

class TimeRecordingUtilities(
    private val persistence: ITimeEntryPersistence,
    val cu: CurrentUser,
    private val logger: ILogger,
    private val rc: IRolesChecker = RolesChecker(cu)
) :
    ITimeRecordingUtilities {

    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        rc.checkAllowed(Role.SYSTEM)
        return TimeRecordingUtilities(persistence, cu, logger, RolesChecker(cu))
    }

    override fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)

        return createOrModifyEntry(entry) {
            val newTimeEntry = persistence.persistNewTimeEntry(entry)
            logger.logAudit(cu) { "Creating new time entry: ${newTimeEntry.shortString()}" }
            newTimeEntry
        }
    }

    override fun changeEntry(entry: TimeEntry): RecordTimeResult{
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return createOrModifyEntry(entry.toTimeEntryPreDatabase()) {
            val oldEntry = persistence.findTimeEntryById(entry.id)
            val newTimeEntry = persistence.overwriteTimeEntry(entry)
            logger.logAudit(cu) { "overwriting old entry with new entry. old: ${oldEntry.shortString()}  new: ${newTimeEntry.shortString()}"}
            newTimeEntry
        }
    }

    private fun createOrModifyEntry(entry: TimeEntryPreDatabase, behavior: () -> TimeEntry): RecordTimeResult{
        val user = cu
        // ensure time entry user is the logged in user, or
        // is the system
        if (user.employee != entry.employee) {
            logger.logWarn(cu) {"time was not recorded successfully: current user ${user.name.value} does not have access " +
                    "to modify time for ${entry.employee.name.value}"}
            return RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null)
        }
        confirmLessThan24Hours(entry.time, entry.employee, entry.date)
        if(persistence.isInASubmittedPeriod(entry.employee, entry.date)){
            return RecordTimeResult(StatusEnum.LOCKED_ALREADY_SUBMITTED)
        }
        return try {
            val newTimeEntry = behavior()
            RecordTimeResult(StatusEnum.SUCCESS, newTimeEntry)
        } catch (ex : IllegalStateException) {
            logger.logDebug(cu) {"Error adding time entry: ${ex.message}"}

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
        logger.logDebug(cu) {"confirming total time is less than 24 hours"}
        // make sure the employee has a total (new plus existing) of less than 24 hours
        val minutesRecorded : Time = persistence.queryMinutesRecorded(employee, date)
        val twentyFourHours = 24 * 60
        // If the employee is entering in more than 24 hours in a day, that's invalid.
        val existingPlusNewMinutes = minutesRecorded.numberOfMinutes + time.numberOfMinutes
        if (existingPlusNewMinutes > twentyFourHours) {
            logger.logDebug(cu) {"More minutes entered ($existingPlusNewMinutes) than exists in a day (1440)"}
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
        rc.checkAllowed(Role.ADMIN, Role.SYSTEM)
        require(persistence.getProjectByName(projectName) == NO_PROJECT) {"Cannot create a new project if one already exists by that same name"}
        logger.logAudit(cu) {"Creating a new project, \"${projectName.value}\""}

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
        rc.checkAllowed(Role.ADMIN, Role.SYSTEM)
        val newEmployee = persistence.persistNewEmployee(employeename)
        logger.logAudit(cu) {"Created a new employee, \"${newEmployee.name.value}\""}
        return newEmployee
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.readTimeEntriesOnDate(employee, date)
    }

    override fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.readTimeEntries(employee)
    }

    override fun listAllProjects(): List<Project> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getAllProjects()
    }

    override fun findProjectById(id: ProjectId): Project {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getProjectById(id)
    }

    override fun findEmployeeById(id: EmployeeId): Employee {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getEmployeeById(id)
    }

    override fun listAllEmployees(): List<Employee> {
        rc.checkAllowed(Role.SYSTEM, Role.REGULAR, Role.APPROVER, Role.ADMIN, Role.NONE)
        return persistence.getAllEmployees()
    }

    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        logger.logAudit (cu) { "Submitting time period: ${timePeriod.start.stringValue} to ${timePeriod.end.stringValue}" }
        return persistence.persistNewSubmittedTimePeriod(checkNotNull(cu.employee), timePeriod)
    }

    override fun unsubmitTimePeriod(timePeriod: TimePeriod) {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val submittedPeriod = persistence.getSubmittedTimePeriod(checkNotNull(cu.employee), timePeriod)
        logger.logAudit (cu) { "Unsubmitting time period: ${timePeriod.start.stringValue} to ${timePeriod.end.stringValue}" }
        return persistence.unsubmitTimePeriod(submittedPeriod)
    }

    override fun getSubmittedTimePeriod(timePeriod: TimePeriod) : SubmittedPeriod {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getSubmittedTimePeriod(checkNotNull(cu.employee), timePeriod)
    }

    override fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getTimeEntriesForTimePeriod(employee, timePeriod)
    }

    override fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.isInASubmittedPeriod(employee, date)
    }

    override fun deleteTimeEntry(timeEntry: TimeEntry): Boolean {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val didDelete = persistence.deleteTimeEntry(timeEntry)
        if (!didDelete) {
            throw IllegalStateException("Attempted to delete a non-existent time entry by id")
        }
        return true
    }

    override fun findTimeEntryById(id: TimeEntryId): TimeEntry {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN, Role.SYSTEM)
        return persistence.findTimeEntryById(id)
    }
}