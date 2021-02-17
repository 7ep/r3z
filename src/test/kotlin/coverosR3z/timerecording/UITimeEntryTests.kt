package coverosR3z.timerecording

import coverosR3z.bddframework.BDD
import coverosR3z.misc.DEFAULT_DATE
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.misc.types.*
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.timerecording.types.MAX_DETAILS_LENGTH
import coverosR3z.timerecording.types.TimeEntry
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver

class UITimeEntryTests {

    @BDD
    @Category(UITestCategory::class)
    @Test
    fun `timeentry - An employee should be able to enter time for a specified date`() {
        val s = TimeEntryUserStory.getScenario("timeentry - An employee should be able to enter time for a specified date")

        loginAsUserAndCreateProject("alice", "projecta")
        s.markDone("Given the employee worked 8 hours yesterday,")

        enterTimeForEmployee("projecta")
        s.markDone("when the employee enters their time,")

        verifyTheEntry()
        s.markDone("then time is saved.")

        logout()
    }

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
    @Category(UITestCategory::class)
    @Test
    fun `all inputs should restrict to valid values` () {
        val projecta = "projecta"
        val projectb = "projectb"
        val dateString = pom.calcDateString(DEFAULT_DATE)
        loginAsUserAndCreateProject("alice", projecta)

        // Create another project
        pom.epp.enter(projectb)

        noProjectOrTime()
        includesProjectButNoTime(projecta)
        includesTimeButNoProject()
        badDateSyntax(projecta)
        badDateBefore1980(projecta)
        badDateAfter2200(projecta)

        timeBelowZero(projecta, dateString)
        timeAboveTwentyFour(projecta, dateString)
        timeNotOnValidDivision(projecta, dateString)
        invalidSyntaxOnTimeInput(projecta, dateString)

        // this one actually does create a time entry
        val timeEntry = badDescriptionEntry(projecta, dateString)

        setNoTimeOnEdit(timeEntry)
        badDateSyntaxOnEdit(timeEntry)
        badDateBefore1980OnEdit(timeEntry)
        badDateAfter2200OnEdit(timeEntry)

        timeBelowZeroOnEdit(timeEntry)
        timeAboveTwentyFourOnEdit(timeEntry)
        timeNotOnValidDivisionOnEdit(timeEntry)
        invalidSyntaxOnTimeInputOnEdit(timeEntry)
    }


//    fun template(pom : PageObjectModelLocal) {
//        pom.driver.get("${pom.domain}/${ViewTimeAPI.path}")
//        val createTimeEntryRow = pom.driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
//        val projectSelector = createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()))
//        projectSelector.findElement(By.xpath("//option[. = '$project']")).click()
//        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.TIME_INPUT.getElemName())).sendKeys(time)
//        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DETAIL_INPUT.getElemName())).sendKeys(details)
//        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DATE_INPUT.getElemName())).sendKeys(date)
//        createTimeEntryRow.findElement(By.className(ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass())).click()
//        // we verify the time entry is registered later, so only need to test that we end up on the right page successfully
//        assertEquals("your time entries", driver.title)
//    }


    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `date entry should default to today's date`() {

    }

    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `should group time entries by period`() {

    }

    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `should allow sorting time entries by any field`() {

    }

    @BDD
    @Category(UITestCategory::class)
    @Test
    fun `timeentry - should be able to submit time for a certain period`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to submit time for a certain period")
        addSomeTimeEntries()
        s.markDone("Given that I am done entering my time for the period")

        submitTheEntries()
        s.markDone("When I submit my time")

        checkEntriesAreSubmitted()
        s.markDone("Then the time period is ready to be approved")
    }

    @BDD
    @Category(UITestCategory::class)
    @Test
    fun `timeentry - should be able to unsubmit a period`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to unsubmit a period")

        addSomeTimeEntries()
        submitTheEntries()
        s.markDone("Given that I had submitted my time but need to make a change")

        unsubmitEntries()
        s.markDone("When I unsubmit my time")

        checkPeriodIsUnlocked()
        s.markDone("Then the time period is ready for more editing")
    }

    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `should be able to edit make multiple entries at once`() {
        // Given I spent half my day on training and half on client work
        // There should be fields available to record both these things

        // I put info in both

        // my time has been recorded
    }

    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `a blind person should have equivalent accommodation for entering time`() {
        // I am blind, so I use a screen reader
        // I enter my time using the screen reader
        // and my time is entered
    }

    @BDD
    @Category(UITestCategory::class)
    @Test
    @Ignore("for now, projects allow future entry, this will require changes to the project data structure")
    fun `timeentry - should be possible to disallow time entry on future days for certain projects`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be possible to disallow time entry on future days for certain projects")
        s.markDone("Given I am working on a project for the government that disallows forward entry")
        s.markDone("when I try to enter time tomorrow")
        s.markDone("then the system disallows it.")
    }

    @BDD
    @Category(UITestCategory::class)
    @Test
    fun `timeentry - I should see my existing time entries when I open the time entry page`() {
        val s = TimeEntryUserStory.getScenario("timeentry - I should see my existing time entries when I open the time entry page")

        addSomeTimeEntries()
        s.markDone("Given I had previous entries this period")

        verifyTimeEntries()
        s.markDone("when I open the time entry page")
        s.markDone("then I see my prior entries")
    }

    @Category(UITestCategory::class)
    @Test
    fun `timeentry - I should be able to view previous time periods when viewing entries`() {
        val s = TimeEntryUserStory.getScenario("timeentry - I should be able to view previous time periods when viewing entries")

        addSomeTimeEntries()
        makeSureWereOnANewPeriod()
        s.markDone("Given I have made entries in a previous period")

        navigateToPreviousPeriod()
        s.markDone("When I go to review them")

        verifySubmissionsAreThere()
        s.markDone("Then I can see my entries")
    }

    /**
     * Just to confirm that I am allowed to enter all the time in the future I
     * need for PTO
     */
    @Category(UITestCategory::class)
    @Test
    @Ignore("for now, projects allow future entry, this will require changes to the project data structure")
    fun `I should be able to enter future time on PTO`() {

    }

    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `I should not need to enter details on a time entry`() {

    }

    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `all data entry should be automatically saved as I work`() {

    }

    /**
     * Assuming that the user has previous submitted time periods,
     * when they see the new current time period it has projects
     * pre-populated to the last period's projects.
     */
    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `a new time period should typically populate default projects`() {

    }

    /**
     * The idea here being, for example, maybe there is a checkbox
     * where, if it's checked, tabbing moves from day to day on the same
     * project.  If unchecked, tabbing moves from project to project in
     * a single day
     */
    @Category(UITestCategory::class)
    @Test
    @Ignore("Not started yet")
    fun `should be an option to quickly enter time per project or per day `() {

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
        private const val port = 4003
        private lateinit var pom : PageObjectModelLocal

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
        pom = startupTestForUI(port = port)
    }

    @After
    fun cleanup() {
        pom.fs.shutdown()
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }

    private fun enterTimeForEmployee(project: String) {
        pom.vtp.enterTime(project, "1", "", pom.calcDateString(DEFAULT_DATE))
    }

    private fun enterThreeEntriesForEmployee(project: String) {
        val date = Date(2021, Month.JAN, 1)

        // Enter time
        pom.vtp.enterTime(project, "1", "", pom.calcDateString(date))
        pom.vtp.enterTime(project, "1", "", pom.calcDateString(Date(date.epochDay + 1)))
        pom.vtp.enterTime(project, "1", "", pom.calcDateString(Date(date.epochDay + 2)))
    }



    private fun loginAsUserAndCreateProject(user: String, project: String) {
        val password = DEFAULT_PASSWORD.value

        // register and login
        pom.rp.register(user, password, "Administrator")
        pom.lp.login(user, password)

        // Create project
        pom.epp.enter(project)
    }

    private fun verifyTheEntry() {
        // Verify the entry
        pom.driver.get("${pom.domain}/${ViewTimeAPI.path}?date=$DEFAULT_DATE_STRING")
        assertEquals("your time entries", pom.driver.title)
        assertEquals("2020-06-12", pom.vtp.getDateForEntry(1))
        assertEquals("1.00", pom.vtp.getTimeForEntry(1))
    }

    private fun addSomeTimeEntries() {
        loginAsUserAndCreateProject("alice", "projecta")
        enterThreeEntriesForEmployee("projecta")
    }

    private fun makeSureWereOnANewPeriod() {
        pom.driver.get("${pom.domain}/${ViewTimeAPI.path}?date=2021-01-16")
    }

    private fun navigateToPreviousPeriod() {
        pom.vtp.goToPreviousPeriod()
    }

    private fun submitTheEntries() {
        pom.vtp.submitTimeForPeriod()
    }

    private fun checkEntriesAreSubmitted() {
        assertTrue(pom.vtp.verifyPeriodIsSubmitted())
    }

    private fun checkPeriodIsUnlocked() {
        assertTrue(pom.vtp.verifyPeriodIsUnsubmitted())
    }

    private fun unsubmitEntries() {
        pom.vtp.unsubmitForTimePeriod()
    }

    private fun verifyTimeEntries() {
        // Verify the entries
        pom.driver.get("${pom.domain}/${ViewTimeAPI.path}?${ViewTimeAPI.Elements.TIME_PERIOD.getElemName()}=2021-01-01")

        assertEquals("1.00", pom.vtp.getTimeForEntry(1))
        assertEquals("2021-01-01",  pom.vtp.getDateForEntry(1))

        assertEquals("1.00",  pom.vtp.getTimeForEntry(2))
        assertEquals("2021-01-02",    pom.vtp.getDateForEntry(2))

        assertEquals("1.00", pom.vtp.getTimeForEntry(3))
        assertEquals("2021-01-03", pom.vtp.getDateForEntry(3))
    }

    private fun verifySubmissionsAreThere() {
        val period = pom.vtp.getCurrentPeriod()
        assertEquals("2021-01-01 - 2021-01-15", period)
    }


    /**
     * If I just click save without doing anything, the project being empty will stop us
     */
    private fun noProjectOrTime() {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
    }

    /**
     * If I click save after entering a project, the time being empty will stop us
     */
    private fun includesProjectButNoTime(project: String) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setProjectForNewEntry(project)
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
    }

    /**
     * If I click save without the project, the project being empty will stop us
     */
    private fun includesTimeButNoProject() {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.setTimeForNewEntry("1")
        pom.vtp.clickCreateNewTimeEntry()

        // Confirm we don't have any new entries because we provided no project or time
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
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
        val isTimeEntriesEmpty = pom.pmd.TimeEntryDataAccess().read { it.isEmpty() }
        assertTrue(isTimeEntriesEmpty)
    }
    /**
     * The description can be up to [coverosR3z.timerecording.types.MAX_DETAILS_LENGTH]
     * anything past that won't get recorded.
     *
     * this will indeed create a time entry, but when we examine the resultant
     * time entry in the database it will only be that max length, everything beyond
     * will get truncated
     */
    private fun badDescriptionEntry(project: String, dateString: String) : TimeEntry {
        pom.vtp.enterTime(project, "1", "a".repeat(MAX_DETAILS_LENGTH + 1), dateString)

        // Confirm our only time entry has a details that's exactly MAX_DETAILS_LENGTH long
        val oneAndOnlyTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(MAX_DETAILS_LENGTH, oneAndOnlyTimeEntry.details.value.length)
        return oneAndOnlyTimeEntry
    }

    /**
     * If we clear the time field and try to save, it won't happen
     */
    private fun setNoTimeOnEdit(expectedTimeEntry : TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "")
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
    }

    /**
     * Everything required has been set (project, time) but the date
     * field has been cleared.
     */
    private fun badDateSyntaxOnEdit(expectedTimeEntry : TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.clearTheDateEntryOnEdit(expectedTimeEntry.id.value)
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
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
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
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
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
    }

    /**
     * Everything required has been set (project, time, date) but the time is too low
     */
    private fun timeBelowZeroOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "-0.25")
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
    }


    /**
     * Everything required has been set (project, time, date) but the time is too high
     */
    private fun timeAboveTwentyFourOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "24.25")
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter 1.23, it shouldn't work.
     */
    private fun timeNotOnValidDivisionOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "1.24")
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
    }

    /**
     * Our time entry only allows 0 to 24 and quarter hours.  If we
     * enter a letter, it shouldn't work
     */
    private fun invalidSyntaxOnTimeInputOnEdit(expectedTimeEntry: TimeEntry) {
        pom.vtp.gotoDate(DEFAULT_DATE_STRING)
        pom.vtp.clickEditTimeEntry(expectedTimeEntry.id.value)
        pom.vtp.setTimeForEditingTimeEntry(expectedTimeEntry.id.value, "a")
        pom.vtp.clickSaveTimeEntry(expectedTimeEntry.id.value)

        // Confirm we still have the unchanged time entry
        val existingTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(expectedTimeEntry, existingTimeEntry)
    }

    /**
     * The description can be up to [coverosR3z.timerecording.types.MAX_DETAILS_LENGTH]
     * anything past that won't get recorded.
     *
     * this will indeed create a time entry, but when we examine the resultant
     * time entry in the database it will only be that max length, everything beyond
     * will get truncated
     */
    private fun badDescriptionEntryOnEdit(project: String, dateString: String) {
        pom.vtp.enterTime(project, "1", "a".repeat(MAX_DETAILS_LENGTH + 1), dateString)

        // Confirm our only time entry has a details that's exactly MAX_DETAILS_LENGTH long
        val oneAndOnlyTimeEntry = pom.pmd.TimeEntryDataAccess().read { it.single() }
        assertEquals(MAX_DETAILS_LENGTH, oneAndOnlyTimeEntry.details.value.length)
    }



}