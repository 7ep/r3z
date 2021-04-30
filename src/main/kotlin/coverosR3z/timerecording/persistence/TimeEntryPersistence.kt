package coverosR3z.timerecording.persistence

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.system.logging.ILogger
import coverosR3z.system.misc.types.Date
import coverosR3z.timerecording.exceptions.MultipleSubmissionsInPeriodException
import coverosR3z.persistence.types.DataAccess
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.types.*

class TimeEntryPersistence(
    private val pmd : PureMemoryDatabase,
    private val cu : CurrentUser = CurrentUser(SYSTEM_USER),
    private val logger: ILogger
) : ITimeEntryPersistence {
    
    private val employeeDataAccess: DataAccess<Employee> = pmd.dataAccess(Employee.directoryName)
    private val projectDataAccess: DataAccess<Project> = pmd.dataAccess(Project.directoryName)
    private val timeEntryDataAccess: DataAccess<TimeEntry> = pmd.dataAccess(TimeEntry.directoryName)
    private val submittedPeriodsDataAccess: DataAccess<SubmittedPeriod> = pmd.dataAccess(SubmittedPeriod.directoryName)

    override fun setCurrentUser(cu: CurrentUser): ITimeEntryPersistence {
        return TimeEntryPersistence(pmd, cu, logger)
    }

    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry {
        isEntryValid(entry)
        return timeEntryDataAccess.actOn { entries ->

            // add the new data
            val newIndex = entries.nextIndex.getAndIncrement()

            logger.logTrace(cu) {"new time-entry index is $newIndex" }
            val newTimeEntry = TimeEntry(
                TimeEntryId(newIndex),
                entry.employee,
                entry.project,
                entry.time,
                entry.date,
                entry.details
            )
            entries.add(newTimeEntry)
            logger.logTrace(cu) {"recorded a new timeEntry: $newTimeEntry"}
            newTimeEntry
        }
    }

    override fun overwriteTimeEntry(newEntry: TimeEntry) : TimeEntry {
        isEntryValid(newEntry.toTimeEntryPreDatabase())
        val oldEntry = timeEntryDataAccess.read {entries ->
            entries.single{it.id == newEntry.id}
        }

        check (oldEntry.employee == newEntry.employee) {"Employee field of a time entry may not be changed"}

        timeEntryDataAccess.actOn { entries -> entries.update(newEntry)}

        logger.logDebug(cu) {"modified an existing timeEntry: $newEntry"}
        logger.logTrace(cu) { "old time-entry is $oldEntry and new time-entry is $newEntry" }
        return newEntry
    }

    /**
     * This will throw an exception if the project or employee in
     * this time entry don't exist in the list of projects / employees
     * or is missing in the time entry
     */
    private fun isEntryValid(entry: TimeEntryPreDatabase) {
        check(getProjectById(entry.project.id) != NO_PROJECT) {timeEntryInvalidNoProject}
        check(getEmployeeById(entry.employee.id) != NO_EMPLOYEE) {timeEntryInvalidNoEmployee}
        check(employeeDataAccess.read { it.any{employee -> employee == entry.employee} }) {timeEntryInvalidBadEmployee}
        check(projectDataAccess.read { it.any{project -> project == entry.project} }) {timeEntryInvalidBadProject}
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return projectDataAccess.actOn { projects ->
            val newProject = Project(ProjectId(projects.nextIndex.getAndIncrement()), ProjectName(projectName.value))
            projects.add(newProject)
            logger.logDebug(cu) { "Recorded a new project, \"${projectName.value}\", id: ${newProject.id.value}, to the database" }
            newProject
        }
    }

    override fun isProjectUsedForTimeEntry(project: Project): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteProject(project: Project): Boolean {
        TODO("Not yet implemented")
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return employeeDataAccess.actOn { employees ->
            val newEmployee = Employee(EmployeeId(employees.nextIndex.getAndIncrement()), EmployeeName(employeename.value))
            employees.add(newEmployee)
            logger.logDebug(cu) { "Recorded a new employee, \"${employeename.value}\", id: ${newEmployee.id.value}, to the database" }
            newEmployee
        }
    }

    override fun queryMinutesRecorded(employee: Employee, date: Date): Time {
        return timeEntryDataAccess.read (
            fun(timeEntries): Time {

                // if the employee hasn't entered any time on this date, return 0 minutes
                val totalMinutes = timeEntries.filter { it.date == date && it.employee == employee }.sumBy { te -> te.time.numberOfMinutes }
                return Time(totalMinutes)
            })
    }

    override fun readTimeEntries(employee: Employee): Set<TimeEntry> {
        return timeEntryDataAccess.read { timeEntries -> timeEntries.filter { it.employee == employee } }.toSet()
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return timeEntryDataAccess.read { timeEntries -> timeEntries.filter { it.employee == employee && it.date == date } }.toSet()
    }

    override fun getProjectByName(name: ProjectName): Project {
        check(projectDataAccess.read { it.count { p -> p.name == name } in 0..1 }) {"There must be 0 or 1 project with name of ${name.value}"}
        return projectDataAccess.read { it.singleOrNull { p -> p.name == name } ?: NO_PROJECT }
    }

    override fun getProjectById(id: ProjectId): Project {
        check(projectDataAccess.read { it.count { p -> p.id == id } in 0..1 }) {"There must be 0 or 1 project with id of $id"}
        return projectDataAccess.read { it.singleOrNull { p -> p.id == id } ?: NO_PROJECT }
    }

    override fun getAllProjects(): List<Project> {
        return projectDataAccess.read { it.toList() }
    }

    override fun getAllEmployees(): List<Employee> {
        return employeeDataAccess.read { it.toList() }
    }

    override fun getEmployeeById(id: EmployeeId): Employee {
        check(employeeDataAccess.read { employees -> employees.count {it.id == id} } in 0..1) {"There must be 0 or 1 employee with id of $id"}
        return employeeDataAccess.read { employees -> employees.singleOrNull {it.id == id} ?: NO_EMPLOYEE }
    }

    override fun getEmployeeByName(employeeName: EmployeeName): Employee {
        check(employeeDataAccess.read { employees -> employees.count {it.name == employeeName} } in 0..1) {"TThere must be 0 or 1 employee with name of ${employeeName.value}"}
        return employeeDataAccess.read { employees -> employees.singleOrNull {it.name == employeeName} ?: NO_EMPLOYEE }
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

    override fun persistNewSubmittedTimePeriod(employee: Employee, timePeriod: TimePeriod): SubmittedPeriod {
        val alreadyExists = submittedPeriodsDataAccess.read { submissions -> submissions.any{ it.employee == employee && it.bounds == timePeriod} }
        if (alreadyExists) {
            throw MultipleSubmissionsInPeriodException("A submission already exists for ${employee.name.value} on $timePeriod")
        }

        return submittedPeriodsDataAccess.actOn{ submissions ->
            val newSubmission = SubmittedPeriod(
                SubmissionId(submissions.nextIndex.getAndIncrement()),
                employee,
                timePeriod,
                ApprovalStatus.UNAPPROVED)
            logger.logDebug(cu) { "Recorded a new time period submission," +
                    " employee id \"${employee.id.value}\", id: ${newSubmission.id.value}, from ${newSubmission.bounds.start.stringValue} to ${newSubmission.bounds.end.stringValue}, " +
                    "to the database" }
            submissions.add(newSubmission)
            newSubmission
        }
    }

    override fun getSubmittedTimePeriod(employee: Employee, timePeriod: TimePeriod): SubmittedPeriod {
        check(submittedPeriodsDataAccess.read { submissions ->
            submissions.count { it.employee == employee && it.bounds == timePeriod } in 0..1
        }) {"There must be either 0 or 1 submitted time periods with employee = $employee and timeperiod = $timePeriod"}
        return submittedPeriodsDataAccess.read { submissions ->
            submissions.singleOrNull { it.employee == employee && it.bounds == timePeriod }
        } ?: NullSubmittedPeriod
    }

    override fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry> {
        return timeEntryDataAccess.read {
            timeEntries ->
                timeEntries.filter { it.employee == employee && timePeriod.contains(it.date)}.toSet()
        }
    }

    override fun unsubmitTimePeriod(stp: SubmittedPeriod) {
        submittedPeriodsDataAccess.actOn { submissions ->
            submissions.remove(stp)
            logger.logDebug(cu) { "Unsubmitted a time period submission, employee id \"${stp.employee.id.value}\", id: ${stp.id.value}, from the database" }
        }
    }

    override fun deleteTimeEntry(timeEntry: TimeEntry): Boolean {
        return timeEntryDataAccess.actOn { timeentries -> timeentries.remove(timeEntry) }
    }

    override fun findTimeEntryById(id: TimeEntryId): TimeEntry {
        check(timeEntryDataAccess.read { timeentries -> timeentries.count { it.id == id } in 0..1}) {"There must be 0 or 1 time entry with id of $id"}
        return timeEntryDataAccess.read { timeentries -> timeentries.singleOrNull{ it.id == id } ?: NO_TIMEENTRY }
    }

    override fun approveTimesheet(stp: SubmittedPeriod): Boolean {
        return submittedPeriodsDataAccess.actOn { submissions -> submissions.update(stp.copy(approvalStatus = ApprovalStatus.APPROVED)) }
    }

    override fun unapproveTimesheet(stp: SubmittedPeriod): Boolean {
        return submittedPeriodsDataAccess.actOn { submissions -> submissions.update(stp.copy(approvalStatus = ApprovalStatus.UNAPPROVED)) }
    }

    companion object {
        const val timeEntryInvalidNoProject = "a time entry with no project is invalid"
        const val timeEntryInvalidNoEmployee = "a time entry with no employee is invalid"
        const val timeEntryInvalidBadEmployee = "a time entry with a non-registered employee is invalid"
        const val timeEntryInvalidBadProject = "a time entry with a non-registered project is invalid"
    }
}