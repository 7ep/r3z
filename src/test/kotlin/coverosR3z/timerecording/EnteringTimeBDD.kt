package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.authentication.AuthenticationPersistence
import coverosR3z.authentication.AuthenticationUtilities
import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.*
import org.junit.Test

// personas are listed in the personas.md file

/**
 * Feature: Entering time
 *
 * Employee story:
 *      As an employee, Andrea
 *      I want to record my time
 *      So that I am easily able to document my time in an organized way
 */
class EnteringTimeBDD {

    /**
     * Just a happy path for entering a time entry
     */
    @Test
    fun `I can add a time entry`() {
        // Given I am logged in
        val (tru, _) = initializeAUserAndLogin()

        // When I add a time entry
        val entry = createTimeEntryPreDatabase(Employee(EmployeeId(1), EmployeeName("Alice")))
        val result = tru.recordTime(entry)

        // Then it proceeds successfully
        assertEquals(StatusEnum.SUCCESS, result.status)
    }

    /**
     * Just another flavor of happy path
     */
    @Test
    fun `A employee enters six hours on a project with copious notes`() {
        // Given I have worked 6 hours on project "A" on Monday with a lot of notes
        val (tru, entry) = addingProjectHoursWithNotes()

        // when I enter in that time
        val recordStatus = tru.recordTime(entry)

        // then the system indicates it has persisted the new information
        assertEquals("the system indicates it has persisted the new information", StatusEnum.SUCCESS, recordStatus.status)
    }

    @Test
    fun `A employee has already entered 24 hours for the day, they cannot enter more time on a new entry`() {
        // given the employee has already entered 24 hours of time entries before
        val (tru, newProject: Project, newEmployee: Employee) = enterTwentyFourHoursPreviously()

        // when they enter in a new time entry for one hour
        val entry = createTimeEntryPreDatabase(time = Time(30), project = newProject, employee = newEmployee)

        // then the system disallows it
        assertThrows(ExceededDailyHoursAmountException::class.java) { tru.recordTime(entry) }
    }

//    fun editEntry(newTime: Time, newDetails: Details = Details("")) {
//        return TimeEntryPreDatabase()
//    }

//    @Test
//    fun `An employee should be able to edit the number of hours worked from a previous time entry` () {
//        //given Andrea has a previous time entry with 24 hours
//        val pmd = PureMemoryDatabase()
//        val authPersistence = AuthenticationPersistence(pmd)
//        val au = AuthenticationUtilities(authPersistence)
//
//        val systemTru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
//        val alice = systemTru.createEmployee(EmployeeName("Alice"))
//        val userName = UserName("alice_1")
//
//        au.register(userName, DEFAULT_PASSWORD, alice.id)
//        val (_, user) = au.login(userName, DEFAULT_PASSWORD)
//
//        val project = systemTru.createProject(DEFAULT_PROJECT_NAME)
//
//        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(user))
//
//        assertTrue("Registration must have succeeded", au.isUserRegistered(userName))
//
//        val twentyFourHours = 24*60
//        val entry = createTimeEntryPreDatabase(time = Time(twentyFourHours),
//            project = project,
//            employee = alice)
//
//        val descr = "no problem here"
//        // when the employee enters their time
//        val data: Map<String, String> = mapOf(EnterTimeAPI.Elements.PROJECT_INPUT.elemName to entry.project.id.value.toString(),
//            EnterTimeAPI.Elements.TIME_INPUT.elemName to entry.time.numberOfMinutes.toString(),
//            EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.stringValue,
//            EnterTimeAPI.Elements.DETAIL_INPUT.elemName to descr)
//        val result = EnterTimeAPI.handlePOST(tru, user.employeeId, data = data)
//
//        val newTime = Time(7)
//        val newDetails = Details("The conditions have changed")
//        val oldEntry = tru.getEntriesForEmployeeOnDate(alice.id, A_RANDOM_DAY_IN_JUNE_2020)
//
//        //when she changes the entry to only 8 hours
//        val newEntry = TimeEntryPreDatabase(alice, project, newTime, A_RANDOM_DAY_IN_JUNE_2020, Details(descr))
//        tru.changeEntry(A_RANDOM_DAY_IN_JUNE_2020, project, newEntry)
//
//        //then it is reflected in the database
//        assertTrue(tru.getEntriesForEmployeeOnDate(alice.id, A_RANDOM_DAY_IN_JUNE_2020).any {
//            it.details == newDetails
//            it.time == newTime
//        })
//    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun addingProjectHoursWithNotes(): Pair<TimeRecordingUtilities, TimeEntryPreDatabase> {
        val pmd = PureMemoryDatabase()
        val authPersistence = AuthenticationPersistence(pmd)
        val au = AuthenticationUtilities(authPersistence)

        val systemTru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(SYSTEM_USER))
        val alice = systemTru.createEmployee(EmployeeName("Alice"))
        val userName = UserName("alice_1")

        au.register(userName, DEFAULT_PASSWORD, alice.id)
        val (_, user) = au.login(userName, DEFAULT_PASSWORD)

        val tru = TimeRecordingUtilities(TimeEntryPersistence(pmd), CurrentUser(user))

        assertTrue("Registration must have succeeded", au.isUserRegistered(userName))

        val newProject = systemTru.createProject(DEFAULT_PROJECT_NAME)

        val entry = createTimeEntryPreDatabase(
                employee = alice,
                project = newProject,
                time = Time(60 * 6),
                details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        )
        return Pair(tru, entry)
    }

    private fun enterTwentyFourHoursPreviously(): Triple<TimeRecordingUtilities, Project, Employee> {
        val pmd = PureMemoryDatabase()
        val timeEntryPersistence = TimeEntryPersistence(pmd)
        val systemTru = TimeRecordingUtilities(timeEntryPersistence, CurrentUser(SYSTEM_USER))
        val newProject = systemTru.createProject(ProjectName("A"))
        val newEmployee = systemTru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val newUsername = UserName(newEmployee.name.value)
        val existingTimeForTheDay = createTimeEntryPreDatabase(employee = newEmployee, project = newProject, time = Time(60 * 24))

        val au = AuthenticationUtilities(AuthenticationPersistence(pmd))
        au.register(newUsername, DEFAULT_PASSWORD, newEmployee.id)
        val (_, user) = au.login(newUsername, DEFAULT_PASSWORD)

        val tru = TimeRecordingUtilities(timeEntryPersistence, CurrentUser(user))
        tru.recordTime(existingTimeForTheDay)
        return Triple(tru, newProject, newEmployee)
    }

}