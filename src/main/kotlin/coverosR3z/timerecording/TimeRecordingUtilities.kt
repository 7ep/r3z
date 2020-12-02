package coverosR3z.timerecording

import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.EmployeeNotRegisteredException
import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.logging.Logger
import coverosR3z.persistence.EmployeeIntegrityViolationException
import coverosR3z.persistence.ProjectIntegrityViolationException

class TimeRecordingUtilities(private val persistence: ITimeEntryPersistence, private val cu : CurrentUser) : ITimeRecordingUtilities {

    private val log = Logger(cu)

    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        return TimeRecordingUtilities(persistence, cu)
    }

    override fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult {
        val user = cu.user
        // ensure time entry user is the logged in user, or
        // is the system
        if (user != SYSTEM_USER && user.employeeId != entry.employee.id) {
            log.info("time was not recorded successfully: current user ${user.name.value} does not have access to modify time for ${entry.employee.name.value}")
            return RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH)
        }
        log.info("Recording ${entry.time.numberOfMinutes} minutes on \"${entry.project.name.value}\"")
        `confirm the employee has a total (new plus existing) of less than 24 hours`(entry)
        return try {
            persistence.persistNewTimeEntry(entry)
            log.debug("recorded time sucessfully")
            RecordTimeResult(StatusEnum.SUCCESS)
        } catch (ex : ProjectIntegrityViolationException) {
            log.debug("time was not recorded successfully: project id did not match a valid project")
            RecordTimeResult(StatusEnum.INVALID_PROJECT)
        } catch (ex : EmployeeIntegrityViolationException) {
            log.debug("time was not recorded successfully: employee id did not match a valid employee")
            RecordTimeResult(StatusEnum.INVALID_EMPLOYEE)
        }
    }

    private fun `confirm the employee has a total (new plus existing) of less than 24 hours`(entry: TimeEntryPreDatabase) {
        log.debug("confirming total time is less than 24 hours")
        // make sure the employee has a total (new plus existing) of less than 24 hours
        val minutesRecorded : Time = try {
            persistence.queryMinutesRecorded(entry.employee, entry.date)
        } catch (ex : EmployeeNotRegisteredException) {
            // if we hit here, it means the employee doesn't exist yet.  For these purposes, that is
            // fine, we are just checking here that if a employee *does* exist, they don't have too many minutes.
            // if they don't exist, just move on through.
            log.debug("employee ${entry.employee} was not registered in the database.  returning 0 minutes recorded.")
            Time(0)
        }

        val twentyFourHours = 24 * 60
        // If the employee is entering in more than 24 hours in a day, that's invalid.
        val existingPlusNewMinutes = minutesRecorded.numberOfMinutes + entry.time.numberOfMinutes
        if (existingPlusNewMinutes > twentyFourHours) {
            log.debug("More minutes entered ($existingPlusNewMinutes) than exists in a day (1440)")
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
        log.info("Creating a new project, \"${projectName.value}\"")

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
        log.info("Creating a new employee, \"${newEmployee.name.value}\"")
        return newEmployee
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
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

}