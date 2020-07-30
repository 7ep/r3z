package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.persistence.ProjectIntegrityViolationException
import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimeEntryPersistenceTests {

    private var tep : ITimeEntryPersistence = TimeEntryPersistence(PureMemoryDatabase())

    @Before fun init() {
        tep = TimeEntryPersistence(PureMemoryDatabase())
    }

    @Test fun `can record a time entry to the database`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName("test employee"))
        tep.persistNewTimeEntry(createTimeEntryPreDatabase(project = newProject, employee = newEmployee))
        val count = tep.readTimeEntries(newEmployee).size
        assertEquals("There should be exactly one entry in the database", 1, count)
    }

    @Test fun `can get all time entries by a employee`() {
        val employeeName = EmployeeName("test")
        val employee = Employee(1, employeeName.value)
        tep.persistNewEmployee(employeeName)
        val newProject: Project = tep.persistNewProject(ProjectName("test project"))
        val entry1 = createTimeEntryPreDatabase(employee = employee, project = newProject)
        val entry2 = createTimeEntryPreDatabase(employee = employee, project = newProject)
        tep.persistNewTimeEntry(entry1)
        tep.persistNewTimeEntry(entry2)
        val expectedResult = listOf(entry1, entry2)

        val actualResult = tep.readTimeEntries(employee)

        val msg = "what we entered and what we get back should be identical, instead got"
        val listOfResultsMinusId = actualResult.map { r -> TimeEntryPreDatabase(r.employee, r.project, r.time, r.date, r.details) }.toList()
        assertEquals(msg, expectedResult, listOfResultsMinusId)

    }

    /**
     * If we try to add a time entry with a project id that doesn't exist in
     * the database, we should get an exception back from the database
     */
    @Test fun `Can't record a time entry that has a nonexistent project id`() {
        assertThrows(ProjectIntegrityViolationException::class.java) {
            tep.persistNewTimeEntry(createTimeEntryPreDatabase())
        }
    }

    /**
     * We need to be able to know how many hours a employee has worked for the purpose of validation
     */
    @Test
    fun `Can query hours worked by a employee on a given day`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName("test employee"))
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )

        val query = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(60, query)
    }

    @Test
    fun `if a employee has not worked on a given day, we return 0 as their minutes worked that day`() {
        val newEmployee: Employee = tep.persistNewEmployee(DEFAULT_EMPLOYEENAME)
        val minutesWorked = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("should be 0 since they didn't work that day", 0, minutesWorked)
    }

    @Test
    fun `If a employee worked 24 hours total in a day, we should get that from queryMinutesRecorded`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName("test employee"))
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 10),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 13),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )

        val query = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("we should get 24 hours worked for this day", 60 * 24, query)
    }


    @Test
    fun `If a employee worked 8 hours a day for two days, we should get just 8 hours when checking one of those days`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName(DEFAULT_EMPLOYEE.name))
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 8),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 8),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE
                )
        )

        val query = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("we should get 8 hours worked for this day", 60 * 8, query)
    }

}