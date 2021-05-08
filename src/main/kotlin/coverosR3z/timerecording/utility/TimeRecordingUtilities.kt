package coverosR3z.timerecording.utility

import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.utility.IRolesChecker
import coverosR3z.system.logging.ILogger
import coverosR3z.system.misc.types.Date
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

    // region timeentries

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
        val oldEntry = persistence.findTimeEntryById(entry.id)
        return createOrModifyEntry(entry.toTimeEntryPreDatabase(), oldEntry = oldEntry) {
            val newTimeEntry = persistence.overwriteTimeEntry(entry)
            logger.logAudit(cu) { "overwriting old entry with new entry. old: ${oldEntry.shortString()}  new: ${newTimeEntry.shortString()}"}
            newTimeEntry
        }
    }

    private fun createOrModifyEntry(entry: TimeEntryPreDatabase, oldEntry: TimeEntry = NO_TIMEENTRY, behavior: () -> TimeEntry): RecordTimeResult{
        val user = cu
        // ensure time entry user is the logged in user, or
        // is the system
        if (user.employee != entry.employee) {
            logger.logWarn(cu) {"time was not recorded successfully: current user ${user.name.value} does not have access " +
                    "to modify time for ${entry.employee.name.value}"}
            return RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null)
        }
        confirmLessThan24Hours(entry.time, entry.employee, entry.date, oldEntry)
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

    private fun confirmLessThan24Hours(time: Time, employee: Employee, date: Date, oldEntry: TimeEntry) {
        logger.logDebug(cu) {"confirming total time is less than 24 hours"}

        // make sure the employee has a total (new plus existing) of less than 24 hours
        val minutesRecorded : Time = persistence.queryMinutesRecorded(employee, date)
        val twentyFourHours = 24 * 60
        // If the employee is entering in more than 24 hours in a day, that's invalid.

        val existingPlusNewMinutes = if (oldEntry == NO_TIMEENTRY) {
            logger.logTrace { "creating a new time entry" }
            minutesRecorded.numberOfMinutes + time.numberOfMinutes
        } else {
            logger.logTrace { "editing a time entry" }
            minutesRecorded.numberOfMinutes - oldEntry.time.numberOfMinutes + time.numberOfMinutes
        }

        if (existingPlusNewMinutes > twentyFourHours) {
            logger.logDebug(cu) {"More minutes entered ($existingPlusNewMinutes) than exists in a day (1440)"}
            throw ExceededDailyHoursAmountException()
        }
    }

    override fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getTimeEntriesForTimePeriod(employee, timePeriod)
    }

    override fun deleteTimeEntry(timeEntry: TimeEntry): Boolean {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val didDelete = persistence.deleteTimeEntry(timeEntry)
        if (!didDelete) {
            throw IllegalStateException("Attempted to delete a non-existent time entry by id")
        }
        logger.logAudit (cu) { "Deleted time entry: ${timeEntry.shortString()} " }
        return true
    }

    override fun findTimeEntryById(id: TimeEntryId): TimeEntry {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN, Role.SYSTEM)
        return persistence.findTimeEntryById(id)
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.readTimeEntriesOnDate(employee, date)
    }

    override fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.readTimeEntries(employee)
    }

    // endregion

    // region projects

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

    override fun listAllProjects(): List<Project> {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getAllProjects()
    }

    override fun findProjectById(id: ProjectId): Project {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getProjectById(id)
    }

    override fun findProjectByName(name: ProjectName): Project {
        return persistence.getProjectByName(name)
    }

    override fun deleteProject(project: Project): DeleteProjectResult {
        require(persistence.getProjectById(project.id) != NO_PROJECT)

        return if (persistence.isProjectUsedForTimeEntry(project)) {
            DeleteProjectResult.USED
        } else {
            persistence.deleteProject(project)
            DeleteProjectResult.SUCCESS
        }
    }

    // endregion

    // region employees
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

    override fun findEmployeeById(id: EmployeeId): Employee {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getEmployeeById(id)
    }

    override fun findEmployeeByName(name: EmployeeName): Employee {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getEmployeeByName(name)
    }

    override fun listAllEmployees(): List<Employee> {
        rc.checkAllowed(Role.SYSTEM, Role.REGULAR, Role.APPROVER, Role.ADMIN, Role.NONE)
        return persistence.getAllEmployees()
    }

    // endregion

    // region submittals

    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val existingSubmission = persistence.getSubmittedTimePeriod(cu.employee, timePeriod)
        check (existingSubmission == NullSubmittedPeriod) { "Cannot submit an already-submitted period" }
        logger.logAudit (cu) { "Submitting time period: ${timePeriod.start.stringValue} to ${timePeriod.end.stringValue}" }
        return persistence.persistNewSubmittedTimePeriod(checkNotNull(cu.employee), timePeriod)
    }

    override fun unsubmitTimePeriod(timePeriod: TimePeriod) {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val submittedPeriod = persistence.getSubmittedTimePeriod(checkNotNull(cu.employee), timePeriod)
        check (submittedPeriod != NullSubmittedPeriod) { "Cannot unsubmit a non-submitted period" }
        logger.logAudit (cu) { "Unsubmitting time period: ${timePeriod.start.stringValue} to ${timePeriod.end.stringValue}" }
        return persistence.unsubmitTimePeriod(submittedPeriod)
    }

    override fun getSubmittedTimePeriod(timePeriod: TimePeriod) : SubmittedPeriod {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.getSubmittedTimePeriod(checkNotNull(cu.employee), timePeriod)
    }

    override fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean {
        rc.checkAllowed(Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return persistence.isInASubmittedPeriod(employee, date)
    }

    // endregion

    // region approvals

    override fun approveTimesheet(employee: Employee, startDate: Date) : ApprovalResultStatus{
        rc.checkAllowed(Role.APPROVER, Role.ADMIN)
        if (employee == NO_EMPLOYEE) {
            logger.logWarn(cu) { "Cannot approve timesheet for NO_EMPLOYEE" }
            return ApprovalResultStatus.FAILURE
        }
        if (! persistence.isInASubmittedPeriod(employee, startDate)) {
            logger.logWarn(cu) { "Cannot approve timesheet for unsubmitted period" }
            return ApprovalResultStatus.FAILURE
        }
        val stp = persistence.getSubmittedTimePeriod(employee, TimePeriod.getTimePeriodForDate(startDate))
        persistence.approveTimesheet(stp)
        logger.logAudit (cu) { "Approved ${employee.name.value}'s timesheet that starts on ${startDate.stringValue}" }
        return ApprovalResultStatus.SUCCESS
    }

    override fun isApproved(employee: Employee, startDate: Date): ApprovalStatus {
        val stp = persistence.getSubmittedTimePeriod(employee, TimePeriod.getTimePeriodForDate(startDate))
        return stp.approvalStatus
    }

    override fun unapproveTimesheet(employee: Employee, startDate: Date): ApprovalResultStatus {
        rc.checkAllowed(Role.APPROVER, Role.ADMIN)
        if (employee == NO_EMPLOYEE) {
            logger.logWarn(cu) { "Cannot unapprove timesheet for NO_EMPLOYEE" }
            return ApprovalResultStatus.FAILURE
        }
        if (! persistence.isInASubmittedPeriod(employee, startDate)) {
            logger.logWarn(cu) { "Cannot unapprove timesheet for unsubmitted period" }
            return ApprovalResultStatus.FAILURE
        }
        val timePeriod = TimePeriod.getTimePeriodForDate(startDate)
        if (persistence.getSubmittedTimePeriod(employee, timePeriod).approvalStatus == ApprovalStatus.UNAPPROVED) {
            logger.logWarn(cu) { "Cannot unapprove an already-unapproved timesheet" }
            return ApprovalResultStatus.FAILURE
        }
        val stp = persistence.getSubmittedTimePeriod(employee, timePeriod)
        persistence.unapproveTimesheet(stp)
        logger.logAudit (cu) { "Unapproved ${employee.name.value}'s timesheet that starts on ${startDate.stringValue}" }
        return ApprovalResultStatus.SUCCESS
    }

    // endregion

}