package coverosR3z.timerecording

import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.system.misc.*
import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.types.Month
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
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import java.io.File

/**
 * A UI test for one of the core ideas in the system - entering time
 */
@Category(UITestCategory::class)
class TimeEntryUITests {

    private val adminUsername = "admin"
    private val adminPassword = "password12345"
    private val defaultProject = "projecta"

    @Test
    fun testWithChrome() {
        init(Drivers.CHROME.driver)
        timeEntryTests()
    }

    @Test
    fun testWithFirefox() {
        init(Drivers.FIREFOX.driver)
        timeEntryTests()
    }

    private fun timeEntryTests() {
        `setup some default projects and employees`()
        `createEmployee - I should be able to create an employee`()
        `timeentry - An employee should be able to enter time for a specified date`()
        `editTime - An employee should be able to edit the number of hours worked from a previous time entry`()
        `timeentry - should be able to submit time for a certain period`()
        `approval - should be able to approve a time period`()
        `timeentry - should not be able to make any edits if approved`()
        `approval - should be able to unapprove time`()
        `timeentry - should be able to unsubmit a period`()
        `approval - should not be able to approve unsubmitted time`()
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

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    fun init(driver: () -> WebDriver) {
        val databaseDirectorySuffix = "uittimeentrytests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory, driver = driver)

        // Each UI test puts the window in a different place around the screen
        // so we have a chance to see what all is going on
        pom.driver.manage().window().position = Point(0, 0)
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

        pom.vtp.editTime(1, "projecta", "2", "", DEFAULT_DATE)
        s.markDone("when she changes the entry to two hours,")

        assertTwoHoursWerePersisted()
        s.markDone("then the system indicates the two hours was persisted")
    }

    private fun `timeentry - An employee should be able to enter time for a specified date`() {
        val s = TimeEntryUserStory.getScenario("timeentry - An employee should be able to enter time for a specified date")

        loginAndrea()
        s.markDone("Given the employee worked 8 hours yesterday,")

        pom.vtp.enterTime("projecta", "1", "", DEFAULT_DATE)
        s.markDone("when the employee enters their time,")

        verifyTheEntry()
        s.markDone("then time is saved.")
    }

    private fun `timeentry - should be able to unsubmit a period`() {
        logout()
        loginAndrea()
        pom.vtp.gotoDate(DEFAULT_DATE)
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to unsubmit a period")
        val a = ApprovalUserStory.getScenario("Approval - unapproved time periods can be unsubmitted")
        s.markDone("Given that I had submitted my time but need to make a change")
        a.markDone("Given a time period had previously been approved but then unapproved")

        pom.vtp.unsubmitForTimePeriod()
        s.markDone("When I unsubmit my time")
        a.markDone("when the employee tries to unsubmit their time")

        assertTrue(pom.vtp.verifyPeriodIsUnsubmitted())
        s.markDone("Then the time period is ready for more editing")
        a.markDone("then they are able to do so")
    }

    private fun `timeentry - I should see my existing time entries when I open the time entry page`() {
        logout()
        loginAndrea()
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

    private fun `approval - should not be able to approve unsubmitted time`() {
        logout()
        pom.lp.login(adminUsername, adminPassword)
        val s = ApprovalUserStory.getScenario("Approval - An approver should not be able to approve unsubmitted time")
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.switchToViewingEmployee(regularEmployeeName)
        val viewingMessage = pom.driver.findElement(By.id("viewing_whose_timesheet")).text
        assertEquals("Viewing Andrea's unsubmitted timesheet", viewingMessage)
        s.markDone("Given an employee had not submitted their time,")

        assertFalse(pom.vtp.isApproved())
        pom.vtp.toggleApproval(regularEmployeeName, DEFAULT_DATE)
        s.markDone("when I try to approve it")

        assertFalse(pom.vtp.isApproved())
        s.markDone("then the approval status remains unchanged")
    }

    /**
     * If a time period is approved, a user won't see a submit button, an edit button,
     * a create button
     */
    private fun `timeentry - should not be able to make any edits if approved`() {
        val s = ApprovalUserStory.getScenario("Approval - approved time periods cannot be unsubmitted")
        s.markDone("Given a time period had been approved")

        logout()
        loginAndrea()
        pom.vtp.gotoDate(DEFAULT_DATE)
        try {
            pom.driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_BUTTON.getId()))
            fail("The create button shouldn't be found")
        } catch (ex: Throwable) {}

        try {
            pom.driver.findElement(By.id(ViewTimeAPI.Elements.EDIT_BUTTON.getId()))
            fail("The create button shouldn't be found")
        } catch (ex: Throwable) {}

        try {
            pom.driver.findElement(By.id(ViewTimeAPI.Elements.SUBMIT_BUTTON.getId()))
            fail("The submit button shouldn't be found")
        } catch (ex: Throwable) {}

        s.markDone("when the employee tries to unsubmit their time")
        s.markDone("then they are unable to do so")
        assertTrue("if we got here, we didn't fail earlier steps", true)
    }

    private fun `approval - should be able to unapprove time`() {
        logout()
        pom.lp.login(adminUsername, adminPassword)
        val s = ApprovalUserStory.getScenario("Approval - An approver should be able to unapprove submitted time")
        s.markDone("Given an employee needs to make some changes to previously approved time")

        // unapprove the time period
        pom.vtp.toggleApproval(regularEmployeeName, DEFAULT_DATE)
        s.markDone("when I unapprove that time period")

        assertFalse(pom.vtp.isApproved())
        s.markDone("then the timesheet is unapproved")
    }

    private fun `approval - should be able to approve a time period`() {
        logout()
        pom.lp.login(adminUsername, adminPassword)
        val s = ApprovalUserStory.getScenario("Approval - An approver should be able to approve submitted time")
        s.markDone("Given an employee submitted their time,")

        // approve the time period
        pom.vtp.toggleApproval(regularEmployeeName, DEFAULT_DATE)
        s.markDone("when I approve it")

        assertTrue(pom.vtp.isApproved())
        s.markDone("then the timesheet is approved")
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
        pom.vtp.enterTime("projecta", "1", "", Date(date.epochDay + 17))
        pom.vtp.enterTime("projecta", "1", "", Date(date.epochDay + 18))
        pom.vtp.enterTime("projecta", "1", "", Date(date.epochDay + 19))
        pom.vtp.gotoDate(Date(2021, Month.JAN, 16))
    }

    private fun verifyTheEntry() {
        // Verify the entry
        val id = pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName)
            .read { entries -> entries.single { it.employee.name.value == "Andrea" && it.date == DEFAULT_DATE } }.id.value
        pom.driver.get("${pom.sslDomain}/${ViewTimeAPI.path}?date=$DEFAULT_DATE_STRING")
        assertEquals("Your time entries", pom.driver.title)
        assertEquals("1.00", pom.vtp.getTimeForEntry(id))
    }

    private fun verifyTimeEntries() {
        // Verify the entries
        pom.vtp.gotoDate(Date(2020, Month.JUN, 1))

        // get the id's of our time entries, in order
        val entries =
            pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName).read { entries -> entries.filter { it.employee.name.value == "Andrea" } }
        val ids = entries
                .map { it.id.value }.sorted()

        assertEquals("2.00", pom.vtp.getTimeForEntry(ids[0]))
    }

    private fun verifySubmissionsAreThere() {
        val period = pom.vtp.getCurrentPeriod()
        assertEquals("2021-01-01 - 2021-01-15", period)
    }

}