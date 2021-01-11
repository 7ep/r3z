package coverosR3z.uitests

import coverosR3z.BDD
import coverosR3z.BDDHelpers
import coverosR3z.UITest
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import coverosR3z.timerecording.api.ViewEmployeesAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.openqa.selenium.WebDriver

class UICreateEmployee {

    @BDD
    @UITest
    @Test
    fun `createEmployee - I should be able to create an employee`() {
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


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */


    companion object {
        private const val port = 2004
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

            // setup for BDD
            createEmployee = BDDHelpers("createEmployeeBDD.html")
            recordTime = BDDHelpers("enteringTimeBDD.html")
        }

        @AfterClass
        @JvmStatic
        fun shutDown() {
            createEmployee.writeToFile()
            recordTime.writeToFile()

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

}