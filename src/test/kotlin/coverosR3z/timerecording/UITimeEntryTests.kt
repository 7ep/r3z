package coverosR3z.timerecording

import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.misc.*
import coverosR3z.misc.types.Date
import coverosR3z.misc.types.Month
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.timerecording.types.TimeEntry
import coverosR3z.uitests.Drivers
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.openqa.selenium.Point
import java.io.File

@RunWith(Parameterized::class)
@Category(UITestCategory::class)
class UITimeEntryTests(private val myDriver: Drivers) {

    private val adminUsername = "admin"
    private val adminPassword = "password12345"
    private val defaultProject = "projecta"

    @Test
    fun timeEntryTests() {
        `setup some default projects and employees`()
        `createEmployee - I should be able to create an employee`()
        `timeentry - An employee should be able to enter time for a specified date`()
        `editTime - An employee should be able to edit the number of hours worked from a previous time entry`()
        `timeentry - should be able to submit time for a certain period`()
        `timeentry - should be able to unsubmit a period`()
        `timeentry - I should see my existing time entries when I open the time entry page`()
        `timeentry - I should be able to view previous time periods when viewing entries`()
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
        private const val port = 4000
        private lateinit var pom: PageObjectModelLocal
        private lateinit var databaseDirectory : String
        private const val regularUsername = "andrea"
        private const val regularEmployeeName = "Andrea"

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Any?> {
            return Drivers.values().asList()
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    @Before
    fun init() {
        val databaseDirectorySuffix = "uittimeentryests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory, driver = myDriver.driver)

        // Each UI test puts the window in a different place around the screen
        // so we have a chance to see what all is going on
        pom.driver.manage().window().position = Point(0, 400)
    }

    @After
    fun finish() {
        pom.fs.shutdown()
        val pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }

    private fun `createEmployee - I should be able to create an employee`() {
        val s = CreateEmployeeUserStory.getScenario("createEmployee - I should be able to create an employee")

        s.markDone("Given the company has hired a new employee, Andrea,")

        pom.eep.enter("Andrea")
        s.markDone("when I add her as an employee,")

        pom.pmd.dataAccess<Employee>(Employee.directoryName).read { employees -> employees.any {it.name.value == "Andrea" }}
        s.markDone("then the system persists the data.")

        logout()
    }

    private fun `editTime - An employee should be able to edit the number of hours worked from a previous time entry`() {
        val s = EditTimeUserStory.getScenario("editTime - An employee should be able to edit the number of hours worked from a previous time entry")

        s.markDone("Given Andrea has a previous time entry with 1 hour,")

        pom.vtp.editTime(1, "projecta", "2", "", pom.calcDateString(DEFAULT_DATE))
        s.markDone("when she changes the entry to two hours,")

        assertTwoHoursWerePersisted()
        s.markDone("then the system indicates the two hours was persisted")
    }

    private fun `timeentry - An employee should be able to enter time for a specified date`() {
        val s = TimeEntryUserStory.getScenario("timeentry - An employee should be able to enter time for a specified date")

        loginAndrea()
        s.markDone("Given the employee worked 8 hours yesterday,")

        pom.vtp.enterTime("projecta", "1", "", pom.calcDateString(DEFAULT_DATE))
        s.markDone("when the employee enters their time,")

        verifyTheEntry()
        s.markDone("then time is saved.")
    }

    private fun `timeentry - should be able to unsubmit a period`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to unsubmit a period")
        s.markDone("Given that I had submitted my time but need to make a change")

        pom.vtp.unsubmitForTimePeriod()
        s.markDone("When I unsubmit my time")

        assertTrue(pom.vtp.verifyPeriodIsUnsubmitted())
        s.markDone("Then the time period is ready for more editing")
    }

    private fun `timeentry - I should see my existing time entries when I open the time entry page`() {
        val s = TimeEntryUserStory.getScenario("timeentry - I should see my existing time entries when I open the time entry page")
        s.markDone("Given I had previous entries this period")

        verifyTimeEntries()
        s.markDone("when I open the time entry page")
        s.markDone("then I see my prior entries")
    }

    private fun `timeentry - I should be able to view previous time periods when viewing entries`() {
        val s = TimeEntryUserStory.getScenario("timeentry - I should be able to view previous time periods when viewing entries")

        enterSomeMoreTime()
        s.markDone("Given I have made entries in a previous period")

        pom.vtp.goToPreviousPeriod()
        s.markDone("When I go to review them")

        verifySubmissionsAreThere()
        s.markDone("Then I can see my entries")
    }

    private fun `timeentry - should be able to submit time for a certain period`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to submit time for a certain period")
        s.markDone("Given that I am done entering my time for the period")

        pom.vtp.submitTimeForPeriod()
        s.markDone("When I submit my time")

        assertTrue(pom.vtp.verifyPeriodIsSubmitted())
        s.markDone("Then the time period is ready to be approved")
    }

    private fun loginAndrea() {
        pom.lp.login(regularUsername, DEFAULT_PASSWORD.value)
    }

    private fun `setup some default projects and employees`() {
        logout()
        // register and login the Admin
        val adminEmployee = pom.businessCode.tru.createEmployee(EmployeeName("Administrator"))
        val (_, user) = pom.businessCode.au.registerWithEmployee(UserName(adminUsername), Password(adminPassword), adminEmployee)
        pom.businessCode.au.addRoleToUser(user, Role.ADMIN)

        // create a regular employee / user
        val regularEmployee = pom.businessCode.tru.createEmployee(EmployeeName(regularEmployeeName))
        pom.businessCode.au.registerWithEmployee(UserName(regularUsername), DEFAULT_PASSWORD, regularEmployee)

        pom.lp.login(adminUsername, adminPassword)

        // Create a default project
        pom.epp.enter(defaultProject)
    }

    private fun assertTwoHoursWerePersisted() {
        val newEntry = pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName)
            .read { entries -> entries.single { it.employee.name.value == "Andrea" && it.date == DEFAULT_DATE } }
        val changedTime = pom.vtp.getTimeForEntry(newEntry.id.value)
        assertEquals("2.00", changedTime)
    }

    private fun enterSomeMoreTime() {
        val date = Date(2021, Month.JAN, 1)
        // Enter time
        pom.vtp.enterTime("projecta", "1", "", pom.calcDateString(Date(date.epochDay + 17)))
        pom.vtp.enterTime("projecta", "1", "", pom.calcDateString(Date(date.epochDay + 18)))
        pom.vtp.enterTime("projecta", "1", "", pom.calcDateString(Date(date.epochDay + 19)))
        pom.vtp.gotoDate("2021-01-16")
    }

    private fun verifyTheEntry() {
        // Verify the entry
        val id = pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName)
            .read { entries -> entries.single { it.employee.name.value == "Andrea" && it.date == DEFAULT_DATE } }.id.value
        pom.driver.get("${pom.sslDomain}/${ViewTimeAPI.path}?date=$DEFAULT_DATE_STRING")
        assertEquals("your time entries", pom.driver.title)
        if (! pom.vtp.isMobileWidth()) {
            assertEquals("2020-06-12", pom.vtp.getDateForEntry(id))
        }
        assertEquals("1.00", pom.vtp.getTimeForEntry(id))
    }

    private fun verifyTimeEntries() {
        // Verify the entries
        pom.vtp.gotoDate("2020-06-01")

        // get the id's of our time entries, in order
        val entries =
            pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName).read { entries -> entries.filter { it.employee.name.value == "Andrea" } }
        val ids = entries
                .map { it.id.value }.sorted()

        assertEquals("2.00", pom.vtp.getTimeForEntry(ids[0]))
        if (! pom.vtp.isMobileWidth()) {
            assertEquals("2020-06-12", pom.vtp.getDateForEntry(ids[0]))
        }
    }

    private fun verifySubmissionsAreThere() {
        val period = pom.vtp.getCurrentPeriod()
        assertEquals("2021-01-01 - 2021-01-15", period)
    }

}