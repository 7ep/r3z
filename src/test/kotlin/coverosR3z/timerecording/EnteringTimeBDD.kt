package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.UserName
import coverosR3z.timerecording.exceptions.ExceededDailyHoursAmountException
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

/**
 * See EnteringTimeBDD.md
 */
class EnteringTimeBDD {

    /**
     * Just another flavor of happy path
     */
    @Test
    fun `A employee enters six hours on a project with copious notes`() {
        val (tru, entry) = addingProjectHoursWithNotes()
        b.markDone("Given I have worked 6 hours on project A on Monday with a lot of notes,")

        val recordStatus = tru.recordTime(entry)
        b.markDone("when I enter in that time,")

        assertEquals("the system indicates it has persisted the new information", StatusEnum.SUCCESS, recordStatus.status)
        b.markDone("then the system indicates it has persisted the new information.")
    }

    @Test
    fun `A employee has already entered 24 hours for the day, they cannot enter more time on a new entry`() {
        val (tru, newProject: Project, newEmployee: Employee) = enterTwentyFourHoursPreviously()
        b.markDone("given the employee has already entered 24 hours of time entries before,")

        val entry = createTimeEntryPreDatabase(time = Time(30), project = newProject, employee = newEmployee)
        b.markDone("when they enter in a new time entry for one hour,")

        assertThrows(ExceededDailyHoursAmountException::class.java) { tru.recordTime(entry) }
        b.markDone("then the system disallows it.")
    }


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    companion object {

        private lateinit var b : BDDHelpers

        @BeforeClass
        @JvmStatic
        fun init() {
            b = BDDHelpers("enteringTimeBDD.html")
        }

        @AfterClass
        @JvmStatic
        fun finishing() {
            b.writeToFile()
        }

    }


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