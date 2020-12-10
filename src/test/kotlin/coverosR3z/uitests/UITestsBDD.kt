package coverosR3z.uitests

import com.gargoylesoftware.htmlunit.BrowserVersion.BEST_SUPPORTED
import coverosR3z.DEFAULT_DATE_STRING
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.LoginAPI
import coverosR3z.logging.LoggingAPI
import coverosR3z.server.NamedPaths
import coverosR3z.server.Server
import coverosR3z.timerecording.EmployeeAPI
import coverosR3z.timerecording.EnterTimeAPI
import coverosR3z.timerecording.ProjectAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import java.io.File
import kotlin.concurrent.thread


private const val domain = "http://localhost:12345"

private enum class Drivers(val driver: () -> WebDriver){
    HTMLUNIT({ HtmlUnitDriver(BEST_SUPPORTED) }),
    FIREFOX({ FirefoxDriver() }),
    CHROME({ ChromeDriver(ChromeOptions().setHeadless(false)) })
}

private val webDriver = Drivers.CHROME

/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class UITests {

    companion object {
        private lateinit var sc : Server
        const val dbDirectory = "build/db/"
        private lateinit var driver: WebDriver
        private lateinit var rp : RegisterPage
        private lateinit var lp : LoginPage
        private lateinit var etp : EnterTimePage
        private lateinit var eep : EnterEmployeePage
        private lateinit var epp : EnterProjectPage

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()

            // wipe out the database
            File(dbDirectory).deleteRecursively()

            // start the server
            thread {
                sc = Server(12345, dbDirectory)
                sc.startServer()
            }

            driver = webDriver.driver()

            rp = RegisterPage(driver)
            lp = LoginPage(driver)
            etp = EnterTimePage(driver)
            eep = EnterEmployeePage(driver)
            epp = EnterProjectPage(driver)
        }

        @AfterClass
        @JvmStatic
        fun shutDown() {
            Server.halfOpenServerSocket.close()
            driver.quit()
        }

    }


    //////////////////////////////
    // Entering Time
    /////////////////////////////

    @Test
    fun `An employee should be able to enter time for a specified date`() {
        // given the employee worked 8 hours yesterday
        // when the employee enters their time
        enterTimeForEmployee()

        // then time is saved
        verifyTheEntry()
    }

    private fun enterTimeForEmployee() {
        val dateString = if (driver is ChromeDriver) {
            "06122020"
        } else {
            DEFAULT_DATE_STRING
        }

        val user = "bob"
        val password = DEFAULT_PASSWORD.value
        val details = ""

        // register and login
        rp.register(user, password, "Administrator")
        lp.login(user, password)

        // Create project
        val project = "Sample Project"
        epp.enter(project)

        // Enter time
        etp.enterTime(project, "60", details, dateString)
    }

    private fun verifyTheEntry() {
        // Verify the entry
        driver.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", driver.title)
        assertEquals("2020-06-12", driver.findElement(By.cssSelector("body > table > tbody > tr > td:nth-child(4)")).text)
    }

    private class EnterTimePage(private val driver: WebDriver) {

        fun enterTime(project: String, time: String, details: String, date: String) {
            driver.get("$domain/${NamedPaths.ENTER_TIME.path}")
            driver.findElement(By.id(EnterTimeAPI.Elements.PROJECT_INPUT.id)).findElement(By.xpath("//option[. = '$project']")).click()
            driver.findElement(By.id(EnterTimeAPI.Elements.TIME_INPUT.id)).sendKeys(time)
            driver.findElement(By.id(EnterTimeAPI.Elements.DETAIL_INPUT.id)).sendKeys(details)
            driver.findElement(By.id(EnterTimeAPI.Elements.DATE_INPUT.id)).sendKeys(date)
            driver.findElement(By.id(EnterTimeAPI.Elements.ENTER_TIME_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }
    private class LoginPage(private val driver: WebDriver) {
        
        fun login(username: String, password: String) {
            driver.get("$domain/${NamedPaths.LOGIN.path}")
            driver.findElement(By.id(LoginAPI.Elements.USERNAME_INPUT.id)).sendKeys(username)
            driver.findElement(By.id(LoginAPI.Elements.PASSWORD_INPUT.id)).sendKeys(password)
            driver.findElement(By.id(LoginAPI.Elements.LOGIN_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class RegisterPage(private val driver: WebDriver) {

        fun register(username: String, password: String, employee: String) {
            driver.get("$domain/${NamedPaths.REGISTER.path}")
            driver.findElement(By.id("username")).sendKeys(username)
            driver.findElement(By.id("password")).sendKeys(password)
            driver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '$employee']")).click()
            driver.findElement(By.id("register_button")).click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class EnterEmployeePage(private val driver: WebDriver) {

        fun enter(employee: String) {
            driver.get("$domain/${NamedPaths.CREATE_EMPLOYEE.path}")
            driver.findElement(By.id(EmployeeAPI.Elements.EMPLOYEE_INPUT.id)).sendKeys(employee)
            driver.findElement(By.id(EmployeeAPI.Elements.CREATE_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class EnterProjectPage(private val driver: WebDriver) {

        fun enter(project: String) {
            driver.get("$domain/${NamedPaths.CREATE_PROJECT.path}")
            driver.findElement(By.id(ProjectAPI.Elements.PROJECT_INPUT.id)).sendKeys(project)
            driver.findElement(By.id(ProjectAPI.Elements.CREATE_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }


}