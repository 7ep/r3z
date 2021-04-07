package coverosR3z.timerecording

import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.misc.*
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
import org.openqa.selenium.chrome.ChromeDriver
import java.io.File

@RunWith(Parameterized::class)
@Category(UITestCategory::class)
class UITimeEntryValidation(private val myDriver: Drivers) {

    private val adminUsername = "admin"
    private val adminPassword = "password12345"
    private val defaultProject = "projecta"


    /**
     * Fields are project, time, details, date
     * What are valid project values?
     * - only existing projects (which the user has access to)
     * time values?
     * - numbers between 0.0 and 24.0
     * - decimals up to 15 minute precision (.25) are allowed
     * - also 0
     * - definitely not negatives
     * details?
     * - unicode
     * - within 500 characters
     * - can be empty
     * date?
     * - MM/DD/YYYY
     * - supplied by html5 widget
     * - in valid day range for month in given year
     * - between 1980 and 2200
     */
    @Test
    fun `all inputs should restrict to valid values`() {
        `setup some default projects and employees`()

        val dateString = pom.calcDateString(DEFAULT_DATE)

        pom.lp.login("bob", DEFAULT_PASSWORD.value)

        noProjectOrTime()
        includesProjectButNoTime(defaultProject)
        includesTimeButNoProject()
        badDateSyntax(defaultProject)
        badDateBefore1980(defaultProject)
        badDateAfter2200(defaultProject)

        timeBelowZero(defaultProject, dateString)
        timeAboveTwentyFour(defaultProject, dateString)
        timeNotOnValidDivision(defaultProject, dateString)
        invalidSyntaxOnTimeInput(defaultProject, dateString)

        // this one actually does create a time entry
        val timeEntry = badDescriptionEntry(defaultProject, dateString)

        setNoTimeOnEdit(timeEntry)
        badDateSyntaxOnEdit(timeEntry)
        badDateBefore1980OnEdit(timeEntry)
        badDateAfter2200OnEdit(timeEntry)

        timeBelowZeroOnEdit(timeEntry)
        timeAboveTwentyFourOnEdit(timeEntry)
        timeNotOnValidDivisionOnEdit(timeEntry)
        invalidSyntaxOnTimeInputOnEdit(timeEntry)
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
        pom.driver.manage().window().position = Point(800, 0)
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
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
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
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * If I click save without the project, the project being empty will stop us
     */
    private fun includesTimeButNoProject() {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
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
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
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
        val pastDateString = if (pom.driver is ChromeDriver) "1979-12-31" else "12311979"
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(pastDateString)
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
        val futureDateString = if (pom.driver is ChromeDriver) "2200-01-01" else "01012200"
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(futureDateString)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided a date before 1980
        assertFalse(anyEntriesForBob())
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZero(project: String, dateString: String) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("-0.25")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(dateString)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFour(project: String, dateString: String) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("24.25")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(dateString)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivision(project: String, dateString: String) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("1.23")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(dateString)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        assertFalse(anyEntriesForBob())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidSyntaxOnTimeInput(project: String, dateString: String) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("1.23")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(dateString)
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
    private fun badDescriptionEntry(project: String, dateString: String): TimeEntry {
        pom.vtp.enterTime(project, "1", "a".repeat(MAX_DETAILS_LENGTH + 1), dateString)

        // Confirm our only time entry has a details that's exactly MAX_DETAILS_LENGTH long
        val oneAndOnlyTimeEntry = bobSingleEntry()
        assertEquals(MAX_DETAILS_LENGTH, oneAndOnlyTimeEntry.details.value.length)
        return oneAndOnlyTimeEntry
    }

    /**
     * If we clear the time field and try to save, it won't happen
     */
    private fun setNoTimeOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "")
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
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.clearTheDateEntryOnEdit(expectedTimeEntry.id.value)
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
        val pastDateString = if (pom.driver is ChromeDriver) "1979-12-31" else "12311979"
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTheDateEntryOnEdit(expectedTimeEntry.id.value, pastDateString)
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
        val futureDateString = if (pom.driver is ChromeDriver) "2200-01-01" else "01012200"
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTheDateEntryOnEdit(expectedTimeEntry.id.value, futureDateString)
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZeroOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "-0.25")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFourOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "24.25")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivisionOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "1.24")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidSyntaxOnTimeInputOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "a")
        pom.vtp.clickSaveTimeEntry()

        // Confirm we still have the unchanged time entry
        assertEquals(expectedTimeEntry, bobSingleEntry())
    }

}