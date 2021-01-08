package coverosR3z.uitests

import coverosR3z.BDDHelpers
import coverosR3z.DEFAULT_DATE_STRING
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.*
import coverosR3z.logging.LogTypes
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.api.HomepageAPI
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import coverosR3z.timerecording.api.ViewEmployeesAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runners.MethodSorters
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UITests {

    /*
    Employee user story:
         As an employee, Andrea
         I want to record my time
         So that I am easily able to document my time in an organized way
    */
    
    @Test
    fun `001 - recordTime - An employee should be able to enter time for a specified date`() {
        loginAsUserAndCreateProject("alice", "projecta")
        recordTime.markDone("Given the employee worked 8 hours yesterday,")

        enterTimeForEmployee("projecta")
        recordTime.markDone("when the employee enters their time,")

        verifyTheEntry()
        recordTime.markDone("then time is saved.")

        logout()
    }

    //TODO: Implement this test for real
    @Test
    fun `002 - recordTime - An employee should be able to edit the number of hours worked from a previous time entry` () {
        loginAsUserAndCreateProject("Andrea", "projectb")
        recordTime.markDone("Given Andrea has a previous time entry with 24 hours,")

        // when the employee enters their time
        enterTimeForEmployee("projectb")

        driver.get("$domain/${ViewTimeAPI.path}")
        recordTime.markDone("when she changes the entry to only 8 hours,")
        // muck with it

        val timeField = driver.findElement(By.cssSelector("#time-entry-1-1 .time input"))
        timeField.sendKeys("120")
        // change time to 120

        driver.get("$domain/${ViewTimeAPI.path}")

        val expected = 60120
        //assertEquals(expected, driver.findElement(By.cssSelector("#time-entry-1-1 .time input")).getAttribute("value"))
        // stopping point 12/10/20: sent keys do not persist when the driver accesses the page again. Won't solve that
        // until we persist it in some way
        logout()
    }

    @Test
    fun `003 - createEmployee - I should be able to create an employee`() {
        createEmployee.markDone("Given the company has hired a new employee, Andrea,")

        rp.register("employeemaker", "password12345", "Administrator")
        lp.login("employeemaker", "password12345")
        eep.enter("a new employee")
        createEmployee.markDone("when I add her as an employee,")

        assertEquals("SUCCESS", driver.title)
        driver.get("$domain/${ViewEmployeesAPI.path}")
        createEmployee.markDone("then the system indicates success.")

        logout()
    }


    @Test
    fun `004 - general - should get a 404 error on page not found`() {
        driver.get("${domain}/does-not-exist")
        assertEquals("404 error", driver.title)
    }

    @Test
    fun `005 - general - should get a 401 error if I fail to login`() {
        lp.login("userabc", "password12345")
        assertEquals("401 error", driver.title)
    }

    @Test
    fun `007 - general - should get a failure message if I register an already-registered user`() {
        rp.register("usera", "password12345", "Administrator")
        rp.register("usera", "password12345", "Administrator")
        assertEquals("FAILURE", driver.title)
    }

    @Test
    fun `008 - general - should be able to see the homepage and the authenticated homepage`() {
        driver.get("${domain}/${HomepageAPI.path}")
        assertEquals("Homepage", driver.title)
        rp.register("employeemaker", "password12345", "Administrator")
        lp.login("employeemaker", "password12345")
        driver.get("${domain}/${HomepageAPI.path}")
        assertEquals("Authenticated Homepage", driver.title)
        logout()
    }

    @Test
    fun `009 - general - I should be able to change the logging settings`() {
        // Given I am an admin
        rp.register("corey", "password12345", "Administrator")
        lp.login("corey", "password12345")
        llp.go()
        // When I set Warn-level logging to not log
        llp.setLoggingFalse(LogTypes.WARN)
        llp.save()
        llp.go()
        // Then that logging is set to not log
        assertFalse(llp.isLoggingOn(LogTypes.WARN))
        logout()
    }

    @Test
    fun `010 - general - Validation should stop me entering invalid input on the registration page`() {
        // validation won't allow it through - missing username
        rp.register("", "password12345", "Administrator")
        assertEquals("register", driver.title)

        // validation won't allow it through - missing password
        rp.register("alice", "", "Administrator")
        assertEquals("register", driver.title)

        // validation won't allow it through - missing employee
        driver.get("$domain/${RegisterAPI.path}")
        driver.findElement(By.id("username")).sendKeys("alice")
        driver.findElement(By.id("password")).sendKeys("password12345")
        driver.findElement(By.id("register_button")).click()
        assertEquals("register", driver.title)

        // validation won't allow it through - username too short
        rp.register("a".repeat(minUserNameSize-1), "password12345", "Administrator")
        assertEquals("register", driver.title)

        // Text entry will stop taking characters at the maximum size, so
        // what gets entered will just be truncated to [maxUserNameSize]
        val tooLongUsername = "a".repeat(maxUserNameSize+1)
        rp.register(tooLongUsername, "password12345", "Administrator")
        assertFalse(pmd.actOnUsers { users -> users.any { it.name.value == tooLongUsername } })

        // validation won't allow it through - password too short
        rp.register("alice", "a".repeat(minPasswordSize-1), "Administrator")
        assertEquals("register", driver.title)

        // Text entry will stop taking characters at the maximum size, so
        // what gets entered will just be truncated to [maxPasswordSize]
        // therefore, if we use a password too long, the system will
        // only record the password that was exactly at max size
        val maxPassword = "a".repeat(maxPasswordSize)
        rp.register("cool", maxPassword + "z", "Administrator")
        lp.login("cool", maxPassword)
        assertEquals("SUCCESS", driver.title)
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
        private lateinit var createEmployee : BDDHelpers
        private lateinit var recordTime : BDDHelpers
        private lateinit var businessCode : BusinessCode
        private lateinit var pmd : PureMemoryDatabase

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()

            // start the server
            sc = Server(12345)
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

            // setup for BDD
            createEmployee = BDDHelpers("createEmployeeBDD.html")
            recordTime = BDDHelpers("enteringTimeBDD.html")
        }

        @AfterClass
        @JvmStatic
        fun shutDown() {
            createEmployee.writeToFile()
            recordTime.writeToFile()
            sc.halfOpenServerSocket.close()
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
        driver.get("$domain/${ViewTimeAPI.path}")
        assertEquals("your time entries", driver.title)
        assertEquals("2020-06-12", driver.findElement(By.cssSelector("body > table > tbody > tr > td:nth-child(4)")).text)
    }


}