package coverosR3z.timerecording.utility

import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Roles
import coverosR3z.authentication.types.SYSTEM_USER
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
        rc.checkAllowed(Roles.SYSTEM)
        return TimeRecordingUtilities(persistence, cu, logger)
    }

    override fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)

        return createOrModifyEntry(entry) {
            val newTimeEntry = persistence.persistNewTimeEntry(entry)
            logger.logDebug(cu) {"recorded time successfully"}
            newTimeEntry
        }
    }

    override fun changeEntry(entry: TimeEntry): RecordTimeResult{
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return createOrModifyEntry(entry.toTimeEntryPreDatabase()) {
            val newTimeEntry = persistence.overwriteTimeEntry(entry)
            logger.logDebug(cu) {"modified time successfully"}
            newTimeEntry
        }
    }

    private fun createOrModifyEntry(entry: TimeEntryPreDatabase, behavior: () -> TimeEntry): RecordTimeResult{
        val user = cu
        // ensure time entry user is the logged in user, or
        // is the system
        if (user.employeeId != entry.employee.id) {
            logger.logAudit(cu) {"time was not recorded successfully: current user ${user.name.value} does not have access " +
                    "to modify time for ${entry.employee.name.value}"}
            return RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null)
        }
        logger.logAudit(cu) {"Recording ${entry.time.numberOfMinutes} minutes on \"${entry.project.name.value}\""}
        confirmLessThan24Hours(entry.time, entry.employee, entry.date)
        if(persistence.isInASubmittedPeriod(entry.employee.id, entry.date)){
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
        rc.checkAllowed(Roles.ADMIN)
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
        rc.checkAllowed(Roles.ADMIN, Roles.SYSTEM)
        require(employeename.value.isNotEmpty()) {"Employee name cannot be empty"}
        val newEmployee = persistence.persistNewEmployee(employeename)
        logger.logAudit(cu) {"Creating a new employee, \"${newEmployee.name.value}\""}
        return newEmployee
    }

    override fun getEntriesForEmployeeOnDate(employeeId: EmployeeId, date: Date): Set<TimeEntry> {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        val employee = persistence.getEmployeeById(employeeId)
        return persistence.readTimeEntriesOnDate(employee, date)
    }

    override fun getAllEntriesForEmployee(employeeId: EmployeeId): Set<TimeEntry> {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        val employee = persistence.getEmployeeById(employeeId)
        return persistence.readTimeEntries(employee)
    }

    override fun listAllProjects(): List<Project> {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.getAllProjects()
    }

    override fun findProjectById(id: ProjectId): Project {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.getProjectById(id)
    }

    override fun findEmployeeById(id: EmployeeId): Employee {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.getEmployeeById(id)
    }

    override fun listAllEmployees(): List<Employee> {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN, Roles.NONE)
        return persistence.getAllEmployees()
    }

    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.persistNewSubmittedTimePeriod(checkNotNull(cu.employeeId), timePeriod)
    }

    override fun unsubmitTimePeriod(timePeriod: TimePeriod) {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        val submittedPeriod = persistence.getSubmittedTimePeriod(checkNotNull(cu.employeeId), timePeriod)
        return persistence.unsubmitTimePeriod(submittedPeriod)
    }

    override fun getSubmittedTimePeriod(timePeriod: TimePeriod) : SubmittedPeriod {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.getSubmittedTimePeriod(checkNotNull(cu.employeeId), timePeriod)
    }

    override fun getTimeEntriesForTimePeriod(employeeId: EmployeeId, timePeriod: TimePeriod): Set<TimeEntry> {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.getTimeEntriesForTimePeriod(employeeId, timePeriod)
    }

    override fun isInASubmittedPeriod(employeeId: EmployeeId, date: Date): Boolean {
        rc.checkAllowed(Roles.REGULAR, Roles.APPROVER, Roles.ADMIN)
        return persistence.isInASubmittedPeriod(employeeId, date)
    }
}