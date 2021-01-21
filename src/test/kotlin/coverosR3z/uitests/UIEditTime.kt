package coverosR3z.uitests

import coverosR3z.bdd.BDD
import coverosR3z.bdd.EditTimeUserStory
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

class UIEditTime {

    @BDD
    @UITest
    @Test
    fun `editTime - An employee should be able to edit the number of hours worked from a previous time entry` () {
        val s = EditTimeUserStory.addScenario(
            "editTime - An employee should be able to edit the number of hours worked from a previous time entry",

            listOf(
                "Given Andrea has a previous time entry with 1 hour,",
                "when she changes the entry to two hours,",
                "then the system indicates the two hours was persisted"
            )
        )

        loginAsUserAndCreateProject("Andrea", "projectb")
        s.markDone("Given Andrea has a previous time entry with 1 hour,")

        enterInitialTime()
        s.markDone("when she changes the entry to two hours,")

        changeTime()
        s.markDone("then the system indicates the two hours was persisted")

        logout()
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
        private const val port = 4001
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


    private fun enterInitialTime() {
        // when the employee enters their time
        enterTimeForEmployee("projectb")
        driver.get("$domain/${ViewTimeAPI.path}")
    }

    private fun changeTime() {
        // put the row into edit mode
        driver.findElement(By.cssSelector("#time-entry-1-1 .${ViewTimeAPI.Elements.EDIT_BUTTON.getElemClass()}")).click()

        val timeField =
            driver.findElement(By.cssSelector("#time-entry-1-1 input[name=${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}]"))
        timeField.clear()
        timeField.sendKeys("120")

        // save the new time
        driver.findElement(By.cssSelector("#time-entry-1-1 .${ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass()}")).click()

        // go to the page
        driver.get("$domain/${ViewTimeAPI.path}")

        // confirm the change
        assertEquals("120", driver.findElement(By.cssSelector("#time-entry-1-1 .time")).text)
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

}