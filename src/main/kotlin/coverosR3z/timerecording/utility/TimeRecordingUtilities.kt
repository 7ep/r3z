package coverosR3z.timerecording.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.utility.IRolesChecker
import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.persistence.types.DataAccess
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.system.logging.ILogger
import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.types.DayOfWeek
import coverosR3z.system.misc.types.calculateSundayDate
import coverosR3z.system.misc.types.dayOfWeekCalc
import coverosR3z.timerecording.exceptions.ExceededDailyHoursAmountException
import coverosR3z.timerecording.exceptions.MultipleSubmissionsInPeriodException
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*

class TimeRecordingUtilities(
    val pmd: PureMemoryDatabase,
    val cu: CurrentUser,
    private val logger: ILogger,
    private val rc: IRolesChecker = RolesChecker
) : ITimeRecordingUtilities {

    private val timeEntryInvalidBadEmployee = "a time entry with a non-registered employee is invalid"
    private val timeEntryInvalidBadProject = "a time entry with a non-registered project is invalid"
    private val employeeDataAccess: DataAccess<Employee> = pmd.dataAccess(Employee.directoryName)
    private val projectDataAccess: DataAccess<Project> = pmd.dataAccess(Project.directoryName)
    private val timeEntryDataAccess: DataAccess<TimeEntry> = pmd.dataAccess(TimeEntry.directoryName)
    private val submittedPeriodsDataAccess: DataAccess<SubmittedPeriod> = pmd.dataAccess(SubmittedPeriod.directoryName)

    /**
     * A special command to change the current user.  Careful
     * who you empower to use this.
     */
    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        return TimeRecordingUtilities(pmd, cu, logger, rc)
    }

    // region timeentries

    override fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)

        return createOrModifyEntry(entry) {
            /**
             * This will throw an exception if the project or employee in
             * this time entry don't exist in the list of projects / employees
             * or is missing in the time entry
             */
            check(findProjectById(entry.project.id) != NO_PROJECT) { timeEntryInvalidBadProject }
            check(findEmployeeById(entry.employee.id) != NO_EMPLOYEE) { timeEntryInvalidBadEmployee }
            val newTimeEntry = timeEntryDataAccess.actOn { entries ->

                // add the new data
                val newIndex = entries.nextIndex.getAndIncrement()

                logger.logTrace(cu) { "new time-entry index is $newIndex" }
                val newTimeEntry = TimeEntry(
                    TimeEntryId(newIndex),
                    entry.employee,
                    entry.project,
                    entry.time,
                    entry.date,
                    entry.details
                )
                entries.add(newTimeEntry)
                logger.logTrace(cu) { "recorded a new timeEntry: $newTimeEntry" }
                newTimeEntry
            }
            logger.logAudit(cu) { "Creating new time entry: ${newTimeEntry.shortString()}" }
            newTimeEntry
        }
    }

    override fun changeEntry(entry: TimeEntry): RecordTimeResult {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val oldEntry = findTimeEntryById(entry.id)
        return createOrModifyEntry(entry.toTimeEntryPreDatabase(), oldEntry = oldEntry) {


            val entryPreDatabase = entry.toTimeEntryPreDatabase()
            /**
             * This will throw an exception if the project or employee in
             * this time entry don't exist in the list of projects / employees
             * or is missing in the time entry
             */
            check(findProjectById(entryPreDatabase.project.id) != NO_PROJECT) { timeEntryInvalidBadProject }
            check(findEmployeeById(entryPreDatabase.employee.id) != NO_EMPLOYEE) { timeEntryInvalidBadEmployee }

            check(oldEntry.employee == entry.employee) { "Employee field of a time entry may not be changed" }

            timeEntryDataAccess.actOn { entries -> entries.update(entry) }

            logger.logDebug(cu) { "modified an existing timeEntry: $entry" }
            logger.logTrace(cu) { "old time-entry is $oldEntry and new time-entry is $entry" }


            logger.logAudit(cu) { "overwriting old entry with new entry. old: ${oldEntry.shortString()}  new: ${entry.shortString()}"}
            entry
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
        if(isInASubmittedPeriod(entry.employee, entry.date)){
            return RecordTimeResult(StatusEnum.LOCKED_ALREADY_SUBMITTED)
        }
        return try {
            val newTimeEntry = behavior()
            RecordTimeResult(StatusEnum.SUCCESS, newTimeEntry)
        } catch (ex : IllegalStateException) {
            logger.logDebug(cu) {"Error adding time entry: ${ex.message}"}

            when (ex.message) {
                timeEntryInvalidBadEmployee -> RecordTimeResult(StatusEnum.INVALID_EMPLOYEE, null)
                timeEntryInvalidBadProject -> RecordTimeResult(StatusEnum.INVALID_PROJECT, null)
                else -> RecordTimeResult(StatusEnum.NULL, null)
            }
        }
    }

    private fun confirmLessThan24Hours(time: Time, employee: Employee, date: Date, oldEntry: TimeEntry) {
        logger.logDebug(cu) {"confirming total time is less than 24 hours"}

        // make sure the employee has a total (new plus existing) of less than 24 hours
        val minutesRecorded : Time =  timeEntryDataAccess.read(
            fun(timeEntries): Time {

                // if the employee hasn't entered any time on this date, return 0 minutes
                val totalMinutes = timeEntries.filter { it.date == date && it.employee == employee }
                    .sumBy { te -> te.time.numberOfMinutes }
                return Time(totalMinutes)
            })
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
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return timeEntryDataAccess.read { timeEntries ->
            timeEntries.filter { it.employee == employee && timePeriod.contains(it.date) }.toSet()
        }
    }

    override fun deleteTimeEntry(timeEntry: TimeEntry): Boolean {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val didDelete = timeEntryDataAccess.actOn { timeentries -> timeentries.remove(timeEntry) }
        if (!didDelete) {
            throw IllegalStateException("failed to find this time entry to delete: ${timeEntry.shortString()}")
        }
        logger.logAudit (cu) { "Deleted time entry: ${timeEntry.shortString()} " }
        return true
    }

    override fun findTimeEntryById(id: TimeEntryId): TimeEntry {
        check(timeEntryDataAccess.read { timeentries -> timeentries.count { it.id == id } in 0..1 }) { "There must be 0 or 1 time entry with id of $id" }
        return timeEntryDataAccess.read { timeentries -> timeentries.singleOrNull { it.id == id } ?: NO_TIMEENTRY }
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return timeEntryDataAccess.read { timeEntries -> timeEntries
            .filter { it.employee == employee && it.date == date } }
            .toSet()
    }

    override fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry> {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return timeEntryDataAccess.read { timeEntries -> timeEntries
            .filter { it.employee == employee } }
            .toSet()
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
        rc.checkAllowed(cu, Role.ADMIN, Role.SYSTEM)
        require(findProjectByName(projectName) == NO_PROJECT) {"Cannot create a new project if one already exists by that same name"}
        logger.logAudit(cu) {"Creating a new project, \"${projectName.value}\""}

        return projectDataAccess.actOn { projects ->
            val newProject = Project(ProjectId(projects.nextIndex.getAndIncrement()), ProjectName(projectName.value))
            projects.add(newProject)
            logger.logDebug(cu) { "Recorded a new project, \"${projectName.value}\", id: ${newProject.id.value}, to the database" }
            newProject
        }
    }

    override fun listAllProjects(): List<Project> {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        return projectDataAccess.read { it.toList() }
    }

    override fun findProjectById(id: ProjectId): Project {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        check(projectDataAccess.read { it.count { p -> p.id == id } in 0..1 }) {"There must be 0 or 1 project with id of $id"}
        return projectDataAccess.read { it.singleOrNull { p -> p.id == id } ?: NO_PROJECT }
    }

    override fun findProjectByName(name: ProjectName): Project {
        check(projectDataAccess.read { it.count { p -> p.name == name } in 0..1 }) {"There must be 0 or 1 project with name of ${name.value}"}
        return projectDataAccess.read { it.singleOrNull { p -> p.name == name } ?: NO_PROJECT }
    }

    override fun deleteProject(project: Project): DeleteProjectResult {
        rc.checkAllowed(cu, Role.ADMIN)
        val foundProject = projectDataAccess.read { it.singleOrNull { p -> p.id == project.id } ?: NO_PROJECT }
        require(foundProject != NO_PROJECT)

        return if (timeEntryDataAccess.read { timeentries -> timeentries.any{ it.project == project } }) {
            DeleteProjectResult.USED
        } else {
            projectDataAccess.actOn { projects -> projects.remove(project) }
            logger.logAudit(cu) { "deleted project: ${project.name.value} id: ${project.id.value}" }
            DeleteProjectResult.SUCCESS
        }
    }

    override fun deleteEmployee(employee: Employee): Boolean {
        rc.checkAllowed(cu, Role.ADMIN)
        logger.logAudit(cu) { "deleted employee: ${employee.name.value} id: ${employee.id.value}" }
        require (employee != NO_EMPLOYEE)
        return employeeDataAccess.actOn { employees -> employees.remove(employee) }
    }

    override fun isProjectUsedForTimeEntry(project: Project): Boolean {
        require (project != NO_PROJECT)
        return timeEntryDataAccess.read { timeentries -> timeentries.any{ it.project == project } }
    }

    override fun getTimeForWeek(employee: Employee, todayDate: Date): Time {
        require(employee != NO_EMPLOYEE)

        val sunday = calculateSundayDate(todayDate)

        // get Sunday as an epoch day
        val sundayED = sunday.epochDay
        require (dayOfWeekCalc(sundayED) == DayOfWeek.Sunday)

        val totalMinutesOverWeek = timeEntryDataAccess.read { timeentries -> timeentries
            .filter { (sundayED..(sundayED + 6)).contains(it.date.epochDay) && it.employee == employee }
            .sumBy { it.time.numberOfMinutes }
        }

        return Time(totalMinutesOverWeek)
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
        rc.checkAllowed(cu, Role.ADMIN, Role.SYSTEM)
        val newEmployee =  employeeDataAccess.actOn { employees ->
            val newEmployee = Employee(EmployeeId(employees.nextIndex.getAndIncrement()), EmployeeName(employeename.value))
            employees.add(newEmployee)
            logger.logDebug(cu) { "Recorded a new employee, \"${employeename.value}\", id: ${newEmployee.id.value}, to the database" }
            newEmployee
        }
        logger.logAudit(cu) {"Created a new employee, \"${newEmployee.name.value}\""}
        return newEmployee
    }

    override fun findEmployeeById(id: EmployeeId): Employee {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        check(employeeDataAccess.read { employees -> employees.count {it.id == id} } in 0..1) {"There must be 0 or 1 employee with id of $id"}
        return employeeDataAccess.read { employees -> employees.singleOrNull {it.id == id} ?: NO_EMPLOYEE }
    }

    override fun findEmployeeByName(name: EmployeeName): Employee {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        check(employeeDataAccess.read { employees -> employees.count {it.name == name} } in 0..1) {"TThere must be 0 or 1 employee with name of ${name.value}"}
        return employeeDataAccess.read { employees -> employees.singleOrNull {it.name == name} ?: NO_EMPLOYEE }
    }

    override fun listAllEmployees(): List<Employee> {
        rc.checkAllowed(cu, Role.SYSTEM, Role.REGULAR, Role.APPROVER, Role.ADMIN, Role.NONE)
        return employeeDataAccess.read { it.toList() }
    }

    // endregion

    // region submittals

    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val existingSubmission = getSubmittedTimePeriod(timePeriod)
        check (existingSubmission == NullSubmittedPeriod) { "Cannot submit an already-submitted period" }
        logger.logAudit (cu) { "Submitting time period: ${timePeriod.start.stringValue} to ${timePeriod.end.stringValue}" }
        val alreadyExists = submittedPeriodsDataAccess.read { submissions -> submissions.any{ it.employee == cu.employee && it.bounds == timePeriod} }
        if (alreadyExists) {
            throw MultipleSubmissionsInPeriodException("A submission already exists for ${cu.employee.name.value} on $timePeriod")
        }

        return submittedPeriodsDataAccess.actOn{ submissions ->
            val newSubmission = SubmittedPeriod(
                SubmissionId(submissions.nextIndex.getAndIncrement()),
                cu.employee,
                timePeriod,
                ApprovalStatus.UNAPPROVED)
            logger.logDebug(cu) { "Recorded a new time period submission," +
                    " employee id \"${cu.employee.id.value}\", id: ${newSubmission.id.value}, from ${newSubmission.bounds.start.stringValue} to ${newSubmission.bounds.end.stringValue}, " +
                    "to the database" }
            submissions.add(newSubmission)
            newSubmission
        }
    }

    override fun unsubmitTimePeriod(timePeriod: TimePeriod) {
        rc.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
        val submittedPeriod = getSubmittedTimePeriod(timePeriod)
        check (submittedPeriod != NullSubmittedPeriod) { "Cannot unsubmit a non-submitted period" }
        logger.logAudit (cu) { "Unsubmitting time period: ${timePeriod.start.stringValue} to ${timePeriod.end.stringValue}" }
        submittedPeriodsDataAccess.actOn { submissions ->
            submissions.remove(submittedPeriod)
            logger.logDebug(cu) { "Unsubmitted a time period submission, employee id \"${submittedPeriod.employee.id.value}\", id: ${submittedPeriod.id.value}, from the database" }
        }
    }

    private fun getSubmittedTimePeriod(timePeriod: TimePeriod, employee: Employee) : SubmittedPeriod {
        check(submittedPeriodsDataAccess.read { submissions ->
            submissions.count { it.employee == employee && it.bounds == timePeriod } in 0..1
        }) {"There must be either 0 or 1 submitted time periods with employee = $employee and timeperiod = $timePeriod"}
        return submittedPeriodsDataAccess.read { submissions ->
            submissions.singleOrNull { it.employee == employee && it.bounds == timePeriod }
        } ?: NullSubmittedPeriod
    }

    override fun getSubmittedTimePeriod(timePeriod: TimePeriod) : SubmittedPeriod {
        return getSubmittedTimePeriod(timePeriod, cu.employee)
    }

    override fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean {
        // The following closure returns a boolean depending on whether the provided date falls within
        // any of the submission date ranges for the provided employee
        return submittedPeriodsDataAccess.read {
                submissions -> submissions
            .filter{it.employee == employee}
            .any{it.bounds.contains(date)}
        }
    }

    // endregion

    // region approvals

    override fun approveTimesheet(employee: Employee, startDate: Date) : ApprovalResultStatus{
        rc.checkAllowed(cu, Role.APPROVER, Role.ADMIN)
        if (employee == NO_EMPLOYEE) {
            logger.logWarn(cu) { "Cannot approve timesheet for NO_EMPLOYEE" }
            return ApprovalResultStatus.FAILURE
        }
        if (! isInASubmittedPeriod(employee, startDate)) {
            logger.logWarn(cu) { "Cannot approve timesheet for unsubmitted period" }
            return ApprovalResultStatus.FAILURE
        }
        val stp = getSubmittedTimePeriod(TimePeriod.getTimePeriodForDate(startDate), employee)
        val isApproved = submittedPeriodsDataAccess.actOn { submissions -> submissions.update(stp.copy(approvalStatus = ApprovalStatus.APPROVED)) }
        return if (isApproved) {
            logger.logAudit (cu) { "Approved ${employee.name.value}'s timesheet that starts on ${startDate.stringValue}" }
            ApprovalResultStatus.SUCCESS
        } else {
            logger.logAudit (cu) { "Failed to approve ${employee.name.value}'s timesheet that starts on ${startDate.stringValue}" }
            ApprovalResultStatus.FAILURE
        }
    }

    override fun isApproved(employee: Employee, startDate: Date): ApprovalStatus {
        val stp = getSubmittedTimePeriod(TimePeriod.getTimePeriodForDate(startDate), employee)
        return stp.approvalStatus
    }

    override fun unapproveTimesheet(employee: Employee, startDate: Date): ApprovalResultStatus {
        rc.checkAllowed(cu, Role.APPROVER, Role.ADMIN)
        if (employee == NO_EMPLOYEE) {
            logger.logWarn(cu) { "Cannot unapprove timesheet for NO_EMPLOYEE" }
            return ApprovalResultStatus.FAILURE
        }
        if (! isInASubmittedPeriod(employee, startDate)) {
            logger.logWarn(cu) { "Cannot unapprove timesheet for unsubmitted period" }
            return ApprovalResultStatus.FAILURE
        }
        val timePeriod = TimePeriod.getTimePeriodForDate(startDate)
        if (getSubmittedTimePeriod(timePeriod, employee).approvalStatus == ApprovalStatus.UNAPPROVED) {
            logger.logWarn(cu) { "Cannot unapprove an already-unapproved timesheet" }
            return ApprovalResultStatus.FAILURE
        }
        val stp = getSubmittedTimePeriod(timePeriod, employee)
        val isUnapproved = submittedPeriodsDataAccess.actOn { submissions -> submissions.update(stp.copy(approvalStatus = ApprovalStatus.UNAPPROVED)) }
        return if (isUnapproved) {
            logger.logAudit (cu) { "Unapproved ${employee.name.value}'s timesheet that starts on ${startDate.stringValue}" }
            ApprovalResultStatus.SUCCESS
        } else {
            logger.logAudit (cu) { "Failed to unapprove ${employee.name.value}'s timesheet that starts on ${startDate.stringValue}" }
            ApprovalResultStatus.FAILURE
        }

    }

    // endregion

}