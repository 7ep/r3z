package coverosR3z.timerecording

import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.misc.*
import coverosR3z.misc.types.Date
import coverosR3z.misc.types.earliestAllowableDate
import coverosR3z.misc.types.latestAllowableDate
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.timerecording.types.MAX_DETAILS_LENGTH
import coverosR3z.timerecording.types.TimeEntry
import coverosR3z.uitests.Drivers
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import java.io.File

/**
 * the time entry page should prevent bad input
 */
@Category(UITestCategory::class)
class UITimeEntryValidation {

    private val adminUsername = "admin"
    private val adminPassword = "password12345"
    private val defaultProject = "projecta"

    @Test
    fun testWithChrome() {
        init(Drivers.CHROME.driver)
        `all inputs should restrict to valid values`()
    }

    @Test
    fun testWithFirefox() {
        init(Drivers.FIREFOX.driver)
        `all inputs should restrict to valid values`()
    }

    private fun `all inputs should restrict to valid values`() {
        `setup some default projects and employees`()

        pom.lp.login("bob", DEFAULT_PASSWORD.value)

        noProjectOrTime()
        includesProjectButNoTime(defaultProject)
        includesTimeButNoProject()
        badDateSyntax(defaultProject)
        badDateBefore1980(defaultProject)
        badDateAfter2200(defaultProject)

        timeBelowZero(defaultProject, DEFAULT_DATE)
        timeAboveTwentyFour(defaultProject, DEFAULT_DATE)
        timeNotOnValidDivision(defaultProject, DEFAULT_DATE)
        invalidSyntaxOnTimeInput(defaultProject, DEFAULT_DATE)
        badDateBeforeCurrentTimePeriod(defaultProject, DEFAULT_DATE)
        badDateAfterCurrentTimePeriod(defaultProject, DEFAULT_DATE)

        // this one actually does create a time entry
        val timeEntry = badDescriptionEntry(defaultProject, DEFAULT_DATE)

        setNoTimeOnEdit(timeEntry)
        badDateSyntaxOnEdit(timeEntry)
        badDateBefore1980OnEdit(timeEntry)
        badDateAfter2200OnEdit(timeEntry)
        badDateBeforeCurrentTimePeriod_OnEdit(timeEntry)
        badDateAfterCurrentTimePeriod_OnEdit(timeEntry)

        timeBelowZeroOnEdit(timeEntry)
        timeAboveTwentyFourOnEdit(timeEntry)
        timeNotOnValidDivisionOnEdit(timeEntry)
        invalidTimeValueOnEdit(timeEntry)
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
        private const val port = 4002
        private lateinit var pom: PageObjectModelLocal
        private lateinit var databaseDirectory : String

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    fun init(driver: () -> WebDriver) {
        val databaseDirectorySuffix = "uittimeentryvalidationtests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory, driver = driver)

        // Each UI test puts the window in a different place around the screen
        // so we have a chance to see what all is going on
        pom.driver.manage().window().position = Point(500, 100)
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

    private fun `setup some default projects and employees`() {
        logout()
        val employee = pom.businessCode.tru.createEmployee(DEFAULT_ADMINISTRATOR_NAME)

        val (_, adminUser) = pom.businessCode.au.registerWithEmployee(
            UserName(adminUsername),
            Password(adminPassword),
            employee
        )
        // register and login the Admin
        pom.businessCode.au.addRoleToUser(adminUser, Role.ADMIN)
        pom.lp.login(adminUsername, adminPassword)

        // register bob the employee
        val bobEmployee = pom.businessCode.tru.createEmployee(EmployeeName("Bob"))
        pom.businessCode.au.registerWithEmployee(
            UserName("bob"),
            DEFAULT_PASSWORD,
            bobEmployee
        )

        // Create a default project
        pom.epp.enter(defaultProject)
        pom.epp.enter("projectb")
        pom.eep.enter("Bob")
        logout()
    }

    /**
     * If I just click save without doing anything, the project being empty will stop us
     */
    private fun noProjectOrTime() {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    private fun anyEntriesForBob(): Boolean {
        return pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName).read { entries -> entries.any { it.employee.name.value == "Bob" } }
    }

    /**
     * If I click save after entering a project, the time being empty will stop us
     */
    private fun includesProjectButNoTime(project: String) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * If I click save without the project, the project being empty will stop us
     */
    private fun includesTimeButNoProject() {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Everything required has been set (project, time) but the date
     * field has been cleared.
     */
    private fun badDateSyntax(project: String) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.clearTheNewEntryDateEntry()
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Everything required has been set (project, time) but the
     * date field is too far in the past
     * @see [earliestAllowableDate]
     */
    private fun badDateBefore1980(project: String) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntryString("1979-12-31")
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided a date before 1980
        assertFalse(anyEntriesForBob())
    }

    /**
     * Everything required has been set (project, time) but the date
     * field is too far in the future
     * @see [latestAllowableDate]
     */
    private fun badDateAfter2200(project: String) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntryString("2200-01-01")
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided a date before 1980
        assertFalse(anyEntriesForBob())
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZero(project: String, date: Date) {
        pom.vtp.gotoDate(date)
        pom.vtp.setTimeForNewEntry("-0.25")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFour(project: String, date: Date) {
        pom.vtp.gotoDate(date)
        pom.vtp.setTimeForNewEntry("24.25")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivision(project: String, date: Date) {
        pom.vtp.gotoDate(date)
        pom.vtp.setTimeForNewEntry("1.23")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidSyntaxOnTimeInput(project: String, date: Date) {
        pom.vtp.gotoDate(date)
        pom.vtp.setTimeForNewEntry("1.23")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Not allowed to enter new time on a date after the current [coverosR3z.timerecording.types.TimePeriod]
     */
    private fun badDateAfterCurrentTimePeriod(project: String, date: Date) {
        pom.vtp.gotoDate(date)
        pom.vtp.setTimeForNewEntry("1.00")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(DEFAULT_DATE_NEXT_PERIOD)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Not allowed to enter new time on a date before the current [coverosR3z.timerecording.types.TimePeriod]
     */
    private fun badDateBeforeCurrentTimePeriod(project: String, date: Date) {
        pom.vtp.gotoDate(date)
        pom.vtp.setTimeForNewEntry("1.00")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(DEFAULT_DATE_PREVIOUS_PERIOD)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * The description can be up to [coverosR3z.timerecording.types.MAX_DETAILS_LENGTH]
     * anything past that won't get recorded.
     *
     * this will indeed create a time entry, but when we examine the resultant
     * time entry in the database it will only be that max length, everything beyond
     * will get truncated
     */
    private fun badDescriptionEntry(project: String, date: Date): TimeEntry {
        pom.vtp.enterTime(project, "1", "a".repeat(MAX_DETAILS_LENGTH + 1), date)

        // Confirm our only time entry has a details that's exactly MAX_DETAILS_LENGTH long
        val oneAndOnlyTimeEntry = bobSingleEntry()
        assertEquals(MAX_DETAILS_LENGTH, oneAndOnlyTimeEntry.details.value.length)
        return oneAndOnlyTimeEntry
    }

    /**
     * If we clear the time field and try to save, it won't happen
     */
    private fun setNoTimeOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry("")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    private fun bobSingleEntry(): TimeEntry {
        return pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName).read { entries -> entries.single { it.employee.name.value == "Bob" } }
    }

    /**
     * Everything required has been set (project, time) but the date
     * field has been cleared.
     */
    private fun badDateSyntaxOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.clearTheDateEntryOnEdit()
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Everything required has been set (project, time) but the
     * date field is too far in the past
     * @see [earliestAllowableDate]
     */
    private fun badDateBefore1980OnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTheDateEntryOnEditString("1979-12-31")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Everything required has been set (project, time) but the date
     * field is too far in the future
     * @see [latestAllowableDate]
     */
    private fun badDateAfter2200OnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTheDateEntryOnEditString("2200-01-01")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * When viewing a particular time period, the UI will only allow
     * us to edit time within the current set of dates of this period
     */
    private fun badDateAfterCurrentTimePeriod_OnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTheDateEntryOnEdit(DEFAULT_DATE_NEXT_PERIOD)
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * see [badDateAfterCurrentTimePeriod_OnEdit]
     */
    private fun badDateBeforeCurrentTimePeriod_OnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTheDateEntryOnEdit(DEFAULT_DATE_PREVIOUS_PERIOD)
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZeroOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry("-0.25")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFourOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry("24.25")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivisionOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry("1.24")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidTimeValueOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry("a")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

}