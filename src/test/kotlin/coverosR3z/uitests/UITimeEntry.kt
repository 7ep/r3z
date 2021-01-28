package coverosR3z.uitests

import coverosR3z.bddframework.BDD
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import coverosR3z.timerecording.api.ViewTimeAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

class UITimeEntry {

    @BDD
    @UITest
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
    @UITest
    @Test
    @Ignore("Not started yet")
    fun `all inputs should restrict to valid values` () {
    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `date entry should default to today's date`() {

    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `should group time entries by period`() {

    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `should allow sorting time entries by any field`() {

    }

    @BDD
    @UITest
    @Test
    @Ignore("Not started yet")
    fun `timeentry - should be able to submit time for a certain period`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to submit time for a certain period")
        s.markDone("Given that I am done entering my time for the period")
        s.markDone("When I submit my time")
        s.markDone("Then the time period is ready to be approved")
    }

    @BDD
    @UITest
    @Test
    @Ignore("Not started yet")
    fun `timeentry - should be able to unsubmit a period`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be able to unsubmit a period")
        s.markDone("Given that I had submitted my time but need to make a change")
        s.markDone("When I unsubmit my time")
        s.markDone("Then the time period is ready for more editing")
    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `should be able to edit make multiple entries at once`() {
        // Given I spent half my day on training and half on client work
        // There should be fields available to record both these things

        // I put info in both

        // my time has been recorded
    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `a blind person should have equivalent accommodation for entering time`() {
        // I am blind, so I use a screen reader
        // I enter my time using the screen reader
        // and my time is entered
    }

    @BDD
    @UITest
    @Test
    @Ignore("for now, projects allow future entry, this will require changes to the project data structure")
    fun `timeentry - should be possible to disallow time entry on future days for certain projects`() {
        val s = TimeEntryUserStory.getScenario("timeentry - should be possible to disallow time entry on future days for certain projects")
        s.markDone("Given I am working on a project for the government that disallows forward entry")
        s.markDone("when I try to enter time tomorrow")
        s.markDone("then the system disallows it.")
    }

    @BDD
    @UITest
    @Test
    fun `timeentry - I should see my existing time entries when I open the time entry page`() {
        val s = TimeEntryUserStory.getScenario("timeentry - I should see my existing time entries when I open the time entry page")
        s.markDone("Given I had previous entries this period")
        s.markDone("when I open the time entry page")
        s.markDone("then I see my prior entries")
    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `should be able to navigate the time periods`() {

    }


    /**
     * Just to confirm that I am allowed to enter all the time in the future I
     * need for PTO
     */
    @UITest
    @Test
    @Ignore("for now, projects allow future entry, this will require changes to the project data structure")
    fun `I should be able to enter future time on PTO`() {

    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `I should not need to enter details on a time entry`() {

    }

    @UITest
    @Test
    @Ignore("Not started yet")
    fun `all data entry should be automatically saved as I work`() {

    }

    /**
     * Assuming that the user has previous submitted time periods,
     * when they see the new current time period it has projects
     * pre-populated to the last period's projects.
     */
    @UITest
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
    @UITest
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
        private const val domain = "http://localhost:$port"
        private val webDriver = Drivers.CHROME
        private lateinit var sc : Server
        private lateinit var driver: WebDriver
        private lateinit var rp : RegisterPage
        private lateinit var lp : LoginPage
        private lateinit var llp : LoggingPage
        private lateinit var etp : EnterTimePage
        private lateinit var eep : EnterEmployeePage
        private lateinit var epp : EnterProjectPage
        private lateinit var lop : LogoutPage
        private lateinit var businessCode : BusinessCode
        private lateinit var pmd : PureMemoryDatabase

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
        // start the server
        sc = Server(port)
        pmd = Server.makeDatabase()
        businessCode = Server.initializeBusinessCode(pmd)
        sc.startServer(businessCode)

        driver = webDriver.driver()

        rp = RegisterPage(driver, domain)
        lp = LoginPage(driver, domain)
        etp = EnterTimePage(driver, domain)
        eep = EnterEmployeePage(driver, domain)
        epp = EnterProjectPage(driver, domain)
        llp = LoggingPage(driver, domain)
        lop = LogoutPage(driver, domain)

    }

    @After
    fun cleanup() {
        sc.halfOpenServerSocket.close()
        driver.quit()
    }

    private fun logout() {
        lop.go()
    }

    private fun enterTimeForEmployee(project: String) {
        val dateString = if (driver is ChromeDriver) {
            "06122020"
        } else {
            DEFAULT_DATE_STRING
        }

        // Enter time
        etp.enterTime(project, "60", "", dateString)
    }

    private fun loginAsUserAndCreateProject(user: String, project: String) {
        val password = DEFAULT_PASSWORD.value

        // register and login
        rp.register(user, password, "Administrator")
        lp.login(user, password)

        // Create project
        epp.enter(project)
    }

    private fun verifyTheEntry() {
        // Verify the entry
        driver.get("$domain/${ViewTimeAPI.path}")
        assertEquals("your time entries", driver.title)
        assertEquals("2020-06-12", driver.findElement(By.cssSelector("#time-entry-1-1 .date")).text)
        assertEquals("60", driver.findElement(By.cssSelector("#time-entry-1-1 .time")).text)
    }


}