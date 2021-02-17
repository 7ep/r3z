package coverosR3z.uitests

import coverosR3z.logging.LogTypes
import coverosR3z.misc.DEFAULT_DATE_STRING
import coverosR3z.server.api.HomepageAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.*
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver

class UISmokeTest {

    /**
     * This test tries to use as many parts of the system as possible in one test
     */
    @Category(UITestCategory::class)
    @Test
    fun `the smoke test`() {
        `Go to an unknown page, expecting a not-found error`()
        `Try logging in with invalid credentials, expecting to be forbidden`()
        `Try posting garbage to the registration endpoint, expecting an error`()
        `Register a new user as the administrator`()
        `Login as that user`()
        `Configure logging to show everything, including trace`()
        `Create 3 projects - first_project, second_project, third_project`()
        `Create 2 employees - matt and byron`()
        `Register each of those employees as users`()
        `Each employee will add 1 time entries each on each project, on a single day`()
        `Each employee edits a time entry`()
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
        private const val port = 9999
        private lateinit var pom : PageObjectModelLocal
        private const val TEST_PASSWORD = "password12345"
        private val testUsers = listOf("matt", "byron")
        private val testProjects = listOf("first_project", "second_project", "third_project")
        private lateinit var dateString : String

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
        dateString = if (pom.driver is ChromeDriver) {
            "06122020"
        } else {
            DEFAULT_DATE_STRING
        }
    }

    @After
    fun cleanup() {
        pom.driver.quit()
    }


    private fun `Each employee edits a time entry`() {
        for (u in testUsers) {
            pom.lp.login(u, TEST_PASSWORD)
            pom.vtp.gotoDate(DEFAULT_DATE_STRING)
            assertEquals("your time entries", pom.driver.title)
            val firstTimeEntry = pom.driver.findElements(By.className(ViewTimeAPI.Elements.READ_ONLY_ROW.getElemClass()))[0]
            firstTimeEntry.findElement(By.className(ViewTimeAPI.Elements.EDIT_BUTTON.getElemClass())).click()

            // new page - editing
            val firstEditableTimeEntry = pom.driver.findElements(By.className(ViewTimeAPI.Elements.EDITABLE_ROW.getElemClass()))[0]
            val idChanged = firstEditableTimeEntry.findElement(By.name(ViewTimeAPI.Elements.ID_INPUT.getElemName())).getAttribute("value")
            val time = firstEditableTimeEntry.findElement(By.name(ViewTimeAPI.Elements.TIME_INPUT.getElemName()))
            time.clear()
            time.sendKeys("2")
            firstEditableTimeEntry.findElement(By.ByClassName(ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass())).click()

            // new page
            val editedTimeEntryRow = pom.driver.findElement(By.id("time-entry-$idChanged"))
            val newTime = editedTimeEntryRow.findElement(By.cssSelector(".time input")).getAttribute("value")
            assertEquals("2.00", newTime)
            logout()
        }
    }

    private fun `Each employee will add 1 time entries each on each project, on a single day`() {
        for (u in testUsers) {
            pom.lp.login(u, TEST_PASSWORD)
            for (p in testProjects) {
                pom.etp.enterTime(p, "1", "", dateString)
            }

            // Verify the entries
            pom.driver.get("${pom.domain}/${ViewTimeAPI.path}")
            assertEquals("your time entries", pom.driver.title)
            val timeEntryRows = pom.driver.findElements(By.className(ViewTimeAPI.Elements.READ_ONLY_ROW.getElemClass()))
            for (row in timeEntryRows) {
                assertEquals("1.00", row.findElement(By.cssSelector(".time input")).getAttribute("value"))
                assertEquals("", row.findElement(By.cssSelector(".details input")).getAttribute("value"))
                assertEquals("2020-06-12", row.findElement(By.cssSelector(".date input")).getAttribute("value"))
            }
            logout()
        }
    }

    private fun `Register each of those employees as users`() {
        for (u in testUsers) {
            pom.rp.register(u, TEST_PASSWORD, u)
        }
    }

    private fun `Create 2 employees - matt and byron`() {
        for (u in testUsers) {
            pom.eep.enter(u)
        }
        logout()
    }

    private fun `Create 3 projects - first_project, second_project, third_project`() {
        for (p in testProjects) {
            pom.epp.enter(p)
        }
    }

    private fun `Configure logging to show everything, including trace`() {
        // Given I am an admin
        pom.llp.go()
        // When I set Trace-level logging to not log
        pom.llp.setLoggingTrue(LogTypes.TRACE)
        pom.llp.save()
        pom.llp.go()

        // Then that logging is set to log
        assertTrue(pom.llp.isLoggingOn(LogTypes.TRACE))
    }

    private fun `Login as that user`() {
        pom.lp.login("employeemaker", TEST_PASSWORD)
        pom.driver.get("${pom.domain}/${HomepageAPI.path}")
        assertEquals("Authenticated Homepage", pom.driver.title)
    }

    private fun `Register a new user as the administrator`() {
        pom.driver.get("${pom.domain}/${HomepageAPI.path}")
        assertEquals("Homepage", pom.driver.title)
        pom.rp.register("employeemaker", TEST_PASSWORD, "Administrator")
    }

    private fun `Try posting garbage to the registration endpoint, expecting an error`() {
        pom.rp.register("usera", "password12345", "Administrator")
        pom.rp.register("usera", "password12345", "Administrator")
        assertEquals("FAILURE", pom.driver.title)
    }

    private fun `Try logging in with invalid credentials, expecting to be forbidden`() {
        pom.lp.login("userabc", TEST_PASSWORD)
        assertEquals("401 error", pom.driver.title)
    }

    private fun `Go to an unknown page, expecting a not-found error`() {
        pom.driver.get("${pom.domain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    private fun logout() {
        pom.lop.go()
    }

}