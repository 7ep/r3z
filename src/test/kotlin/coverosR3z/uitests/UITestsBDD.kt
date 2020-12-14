package coverosR3z.uitests

import coverosR3z.DEFAULT_DATE_STRING
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.server.NamedPaths
import coverosR3z.server.Server
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import java.io.File
import kotlin.concurrent.thread

/**
 * User story:
 *     As a user of r3z
 *     I want to access it through a browser
 *     So that it is convenient
 *
 * These tests will run in alphabetic order, ascending
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UITestsBDD {

    @Test
    fun `001 - An employee should be able to enter time for a specified date`() {
        // given the employee worked 8 hours yesterday
        loginAsUserAndCreateProject("alice", "projecta")
        // when the employee enters their time
        enterTimeForEmployee("projecta")

        // then time is saved
        verifyTheEntry()
        logout()
    }

    @Test
    fun `002 - An employee should be able to edit the number of hours worked from a previous time entry` () {
        //given Andrea has a previous time entry with 24 hours
        loginAsUserAndCreateProject("bob", "projectb")
        // when the employee enters their time
        enterTimeForEmployee("projectb")

        //when she changes the entry to only 8 hours
        driver.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        // muck with it

        val timeField = driver.findElement(By.cssSelector("#time-entry-1-1 .time input"))
        timeField.sendKeys("120")
        // change time to 120

        //then it is reflected in the database
        driver.get("$domain/${NamedPaths.TIMEENTRIES.path}")

        val expected = 60120
        //assertEquals(expected, driver.findElement(By.cssSelector("#time-entry-1-1 .time input")).getAttribute("value"))
        // stopping point 12/10/20: sent keys do not persist when the driver accesses the page again. Won't solve that
        // until we persist it in some way
        logout()
    }

    @Test
    fun `004 - I should be able to create an employee`() {
        rp.register("employeemaker", "password12345", "Administrator")
        lp.login("employeemaker", "password12345")
        eep.enter("a new employee")
        assertEquals("SUCCESS", driver.title)
        driver.get("$domain/${NamedPaths.EMPLOYEES.path}")
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
        private const val domain = "http://localhost:12345"
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

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()

            // start the server
            thread {
                sc = Server(12345)
                sc.startServer()
            }

            driver = webDriver.driver()

            rp = RegisterPage(driver, domain)
            lp = LoginPage(driver, domain)
            etp = EnterTimePage(driver, domain)
            eep = EnterEmployeePage(driver, domain)
            epp = EnterProjectPage(driver, domain)
            llp = LoggingPage(driver, domain)
            lop = LogoutPage(driver, domain)
        }

        @AfterClass
        @JvmStatic
        fun shutDown() {
            Server.halfOpenServerSocket.close()
            driver.quit()
        }

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
        driver.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", driver.title)
        assertEquals("2020-06-12", driver.findElement(By.cssSelector("body > table > tbody > tr > td:nth-child(4)")).text)
    }


}