package coverosR3z.timerecording

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.UserName
import coverosR3z.system.misc.*
import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.types.earliestAllowableDate
import coverosR3z.system.misc.types.latestAllowableDate
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.uitests.Drivers
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import java.io.File

/**
 * the time entry page should prevent bad input
 */
@Category(UITestCategory::class)
class TimeEntryUIValidationTests {

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

        pom.vtp.gotoDate(DEFAULT_DATE)
        val timestamp = getTimestamp()

        // project and time missing
        noProjectOrTime(timestamp)
        includesProjectButNoTime(defaultProject, timestamp)
        includesTimeButNoProject(timestamp)

        // date
        badDateSyntax(defaultProject, timestamp)
        badDateBefore1980(defaultProject, timestamp)
        badDateAfter2200(defaultProject, timestamp)
        correctTheDate()

        // details about time
        timeBelowZero(defaultProject, DEFAULT_DATE, timestamp)
        timeAboveTwentyFour(defaultProject, DEFAULT_DATE, timestamp)
        timeNotOnValidDivision(defaultProject, DEFAULT_DATE, timestamp)
        invalidSyntaxOnTimeInput(defaultProject, DEFAULT_DATE, timestamp)

        // details about date
        badDateBeforeCurrentTimePeriod(defaultProject, DEFAULT_DATE, timestamp)
        badDateAfterCurrentTimePeriod(defaultProject, DEFAULT_DATE, timestamp)

        // this one actually does create a time entry
        val timeEntry = badDescriptionEntry(defaultProject, DEFAULT_DATE)
        pom.vtp.clickEditTimeEntry(timeEntry.id.value)

        val timestampForEdit = getTimestamp()

        // project and time missing
        noProjectOrTime_OnEdit(timestampForEdit)
        includesProjectButNoTime_OnEdit(timestampForEdit)
        includesTimeButNoProject_OnEdit(timestampForEdit)

        // date
        badDateSyntax_OnEdit(timestampForEdit)
        badDateBefore1980_OnEdit(timestampForEdit)
        badDateAfter2200_OnEdit(timestampForEdit)

        // details about time
        timeBelowZero_OnEdit(timestampForEdit)
        timeAboveTwentyFour_OnEdit(timestampForEdit)
        timeNotOnValidDivision_OnEdit(timestampForEdit)
        invalidSyntaxOnTimeInput_OnEdit(timestampForEdit)

        // details about date
        badDateBeforeCurrentTimePeriod_OnEdit(timestampForEdit)
        badDateAfterCurrentTimePeriod_OnEdit(timestampForEdit)

        // valid boundaries for time
        enterZeroHours()
        enterTwentyFourHours()
        val twentyFourHourEntry = confirmZeroAndTwentyFourHoursEntries()
        editTwentyFourHourEntry(twentyFourHourEntry)
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
        pom = startupTestForUI(port = port, directory = databaseDirectory, driver = driver, allLoggingOff = true)

        // Each UI test puts the window in a different place around the screen
        // so we have a chance to see what all is going on
        pom.driver.manage().window().position = Point(700, 0)
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


    /**
     * Change a 24-hour time entry to slightly less - 23.75 hours
     */
    private fun editTwentyFourHourEntry(twentyFourHourEntry: TimeEntry) {
        pom.vtp.editTime(
            twentyFourHourEntry.id.value,
            twentyFourHourEntry.project.name.value,
            "23.75",
            twentyFourHourEntry.details.value,
            twentyFourHourEntry.date
        )
    }

    /**
     * Confirm that the zero-hour and 24-hour entries are in the database
     */
    private fun confirmZeroAndTwentyFourHoursEntries(): TimeEntry {
        val adminTru = pom.businessCode.tru.changeUser(CurrentUser(DEFAULT_ADMIN_USER))
        val employee = adminTru.findEmployeeByName(EmployeeName("Bob"))
        val entries = adminTru.getEntriesForEmployeeOnDate(employee, DEFAULT_DATE_NEXT_PERIOD)
        val twentyFourHourEntry = entries.single { it.time == Time(24 * 60) }
        assertEquals(0, entries.single { it.time == Time(0) }.time.numberOfMinutes)
        return twentyFourHourEntry
    }

    /**
     * Enter a time entry that has 24 hours to it - the maximum allowed for a time entry in a day
     */
    private fun enterTwentyFourHours() {
        pom.vtp.enterTime(defaultProject, "24", "just a regular work entry", DEFAULT_DATE_NEXT_PERIOD)
    }

    /**
     * Enter a time entry that has 0 hours
     */
    private fun enterZeroHours() {
        pom.vtp.enterTime(defaultProject, "0", "just a regular work entry", DEFAULT_DATE_NEXT_PERIOD)
    }

    /**
     * Gets the value of the timestamp meta information on the page
     *
     * Each page is labeled in its header section with a meta element called timestamp,
     * which has the date and time in GMT of the rendering.  That way, we can tell whether
     * we are still looking at the same page or not.
     */
    private fun getTimestamp() : String {
        return pom.driver.findElement(By.cssSelector("meta[name=timestamp]")).getAttribute("content")
    }

    /**
     * assert that the entered timestamp is equal to the page's timestamp
     */
    private fun assertTimestampUnchanged(timestamp: String)  {
        assertEquals(timestamp, pom.driver.findElement(By.cssSelector("meta[name=timestamp]")).getAttribute("content"))
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
    private fun noProjectOrTime(timestamp: String) {
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * If I click save after entering a project, the time being empty will stop us
     */
    private fun includesProjectButNoTime(project: String, timestamp: String) {
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * If I click save without the project, the project being empty will stop us
     */
    private fun includesTimeButNoProject(timestamp: String) {
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry("")
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Everything required has been set (project, time) but the date
     * field has been cleared.
     */
    private fun badDateSyntax(project: String, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.clearTheNewEntryDateEntry()
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Everything required has been set (project, time) but the
     * date field is too far in the past
     * @see [earliestAllowableDate]
     */
    private fun badDateBefore1980(project: String, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntryString("1979-12-31")
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Everything required has been set (project, time) but the date
     * field is too far in the future
     * @see [latestAllowableDate]
     */
    private fun badDateAfter2200(project: String, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntryString("2200-01-01")
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }


    private fun correctTheDate() {
        pom.vtp.setDateForNewEntryString(DEFAULT_DATE.stringValue)
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZero(project: String, date: Date, timestamp: String) {
        pom.vtp.setTimeForNewEntry("-0.25")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFour(project: String, date: Date, timestamp: String) {
        pom.vtp.setTimeForNewEntry("24.25")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivision(project: String, date: Date, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1.23")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidSyntaxOnTimeInput(project: String, date: Date, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1.23")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(date)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Not allowed to enter new time on a date after the current [coverosR3z.timerecording.types.TimePeriod]
     */
    private fun badDateAfterCurrentTimePeriod(project: String, date: Date, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1.00")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(TimePeriod.getTimePeriodForDate(date).getNext().start)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Not allowed to enter new time on a date before the current [coverosR3z.timerecording.types.TimePeriod]
     */
    private fun badDateBeforeCurrentTimePeriod(project: String, date: Date, timestamp: String) {
        pom.vtp.setTimeForNewEntry("1.00")
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.setDateForNewEntry(TimePeriod.getTimePeriodForDate(date).getPrevious().start)
        pom.vtp.clickCreateNewTimeEntry()

        assertTimestampUnchanged(timestamp)
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


    private fun includesTimeButNoProject_OnEdit(timestamp: String) {
        pom.vtp.setProjectForEditingTimeEntry("")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    private fun noProjectOrTime_OnEdit(timestamp: String) {
        pom.vtp.setProjectForEditingTimeEntry("")
        pom.vtp.setTimeForEditingTimeEntry("")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * If we clear the time field and try to save, it won't happen
     */
    private fun includesProjectButNoTime_OnEdit(timestamp: String) {
        pom.vtp.setTimeForEditingTimeEntry("")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    private fun bobSingleEntry(): TimeEntry {
        return pom.pmd.dataAccess<TimeEntry>(TimeEntry.directoryName).read { entries -> entries.single { it.employee.name.value == "Bob" } }
    }

    /**
     * Everything required has been set (project, time) but the date
     * field has been cleared.
     */
    private fun badDateSyntax_OnEdit(timestamp: String) {
        pom.vtp.clearTheDateEntryOnEdit()
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Everything required has been set (project, time) but the
     * date field is too far in the past
     * @see [earliestAllowableDate]
     */
    private fun badDateBefore1980_OnEdit(timestamp: String) {
        pom.vtp.setTheDateEntryOnEditString("1979-12-31")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Everything required has been set (project, time) but the date
     * field is too far in the future
     * @see [latestAllowableDate]
     */
    private fun badDateAfter2200_OnEdit(timestamp: String) {
        pom.vtp.setTheDateEntryOnEditString("2200-01-01")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * When viewing a particular time period, the UI will only allow
     * us to edit time within the current set of dates of this period
     */
    private fun badDateAfterCurrentTimePeriod_OnEdit(timestamp: String) {
        pom.vtp.setTheDateEntryOnEdit(DEFAULT_DATE_NEXT_PERIOD)
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * see [badDateAfterCurrentTimePeriod_OnEdit]
     */
    private fun badDateBeforeCurrentTimePeriod_OnEdit(timestamp: String) {
        pom.vtp.setTheDateEntryOnEdit(DEFAULT_DATE_PREVIOUS_PERIOD)
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZero_OnEdit(timestamp: String) {
        pom.vtp.setTimeForEditingTimeEntry("-0.25")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFour_OnEdit(timestamp: String) {
        pom.vtp.setTimeForEditingTimeEntry("24.25")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivision_OnEdit(timestamp: String) {
        pom.vtp.setTimeForEditingTimeEntry("1.24")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidSyntaxOnTimeInput_OnEdit(timestamp: String) {
        pom.vtp.setTimeForEditingTimeEntry("a")
        pom.vtp.clickSaveTimeEntry()

        assertTimestampUnchanged(timestamp)
    }

}