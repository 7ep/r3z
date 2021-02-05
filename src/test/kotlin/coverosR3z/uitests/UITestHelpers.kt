package coverosR3z.uitests

import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.LogoutAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.logging.LogTypes
import coverosR3z.logging.LoggingAPI
import coverosR3z.server.utility.Server
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.ProjectAPI
import coverosR3z.timerecording.api.ViewTimeAPI
import coverosR3z.webDriver
import org.junit.Assert.assertEquals
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver

enum class Drivers(val driver: () -> WebDriver){
    FIREFOX({ FirefoxDriver() }),
    CHROME({ ChromeDriver(ChromeOptions().setHeadless(false)) }),
    CHROME_HEADLESS({ ChromeDriver(ChromeOptions().setHeadless(true)) }),
}

/**
 * sets up everything needed to run the UI tests:
 * - starts the server on the port provided
 * - creates a new database for the server
 * - initializes the utilities needed
 * - creates a [PageObjectModel] so the UI tests have access to all this stuff,
 *   and so we can have a testing-oriented API
 *   @param port the port our server will run on, and thus the port our client should target
 */
fun startupTestForUI(domain: String = "http://localhost", port : Int, driver: () -> WebDriver = webDriver.driver) : PageObjectModelLocal {
    // start the server
    val server = Server(port)
    val pmd = Server.makeDatabase()
    val businessCode = Server.initializeBusinessCode(pmd)
    server.startServer(businessCode)

    return PageObjectModelLocal.make(driver(), port, businessCode, server, pmd, domain)
}

fun startupTestForUIWithoutServer(domain: String = "", port : Int, driver: () -> WebDriver = webDriver.driver) : PageObjectModel {
    return PageObjectModel.make(driver(), port, domain)
}

class EnterTimePage(private val driver: WebDriver, private val domain : String) {

    fun enterTime(project: String, time: String, details: String, date: String) {
        driver.get("$domain/${ViewTimeAPI.path}")
        val createTimeEntryRow = driver.findElement(By.id(ViewTimeAPI.Elements.CREATE_TIME_ENTRY_ROW.getId()))
        val projectSelector = createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.PROJECT_INPUT.getElemName()))
        projectSelector.findElement(By.xpath("//option[. = '$project']")).click()
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.TIME_INPUT.getElemName())).sendKeys(time)
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DETAIL_INPUT.getElemName())).sendKeys(details)
        createTimeEntryRow.findElement(By.name(ViewTimeAPI.Elements.DATE_INPUT.getElemName())).sendKeys(date)
        createTimeEntryRow.findElement(By.className(ViewTimeAPI.Elements.SAVE_BUTTON.getElemClass())).click()
        // we verify the time entry is registered later, so only need to test that we end up on the right page successfully
        assertEquals("your time entries", driver.title)
    }
}

class LoginPage(private val driver: WebDriver, private val domain : String) {

    fun login(username: String, password: String) {
        driver.get("$domain/${LoginAPI.path}")
        driver.findElement(By.id(LoginAPI.Elements.USERNAME_INPUT.getId())).sendKeys(username)
        driver.findElement(By.id(LoginAPI.Elements.PASSWORD_INPUT.getId())).sendKeys(password)
        driver.findElement(By.id(LoginAPI.Elements.LOGIN_BUTTON.getId())).click()
    }
}

class RegisterPage(private val driver: WebDriver, private val domain : String) {

    fun register(username: String, password: String, employee: String) {
        driver.get("$domain/${RegisterAPI.path}")
        driver.findElement(By.id("username")).sendKeys(username)
        driver.findElement(By.id("password")).sendKeys(password)
        driver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '$employee']")).click()
        driver.findElement(By.id("register_button")).click()
    }
}

class EnterEmployeePage(private val driver: WebDriver, private val domain : String) {

    fun enter(employee: String) {
        driver.get("$domain/${CreateEmployeeAPI.path}")
        driver.findElement(By.id(CreateEmployeeAPI.Elements.EMPLOYEE_INPUT.getId())).sendKeys(employee)
        driver.findElement(By.id(CreateEmployeeAPI.Elements.CREATE_BUTTON.getId())).click()
    }
}

class EnterProjectPage(private val driver: WebDriver, private val domain : String) {

    fun enter(project: String) {
        driver.get("$domain/${ProjectAPI.path}")
        driver.findElement(By.id(ProjectAPI.Elements.PROJECT_INPUT.getId())).sendKeys(project)
        driver.findElement(By.id(ProjectAPI.Elements.CREATE_BUTTON.getId())).click()
    }
}

class LoggingPage(private val driver: WebDriver, private val domain: String) {

    fun go() {
        driver.get("$domain/${LoggingAPI.path}")
    }

    fun setLoggingTrue(lt : LogTypes) {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.getId() + "true")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.getId() + "true")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.getId() + "true")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.getId() + "true")
            else -> throw Exception("Test failed - LogType was $lt")
        }
        driver.findElement(By.id(id)).click()
    }

    fun setLoggingFalse(lt : LogTypes) {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.getId() + "false")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.getId() + "false")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.getId() + "false")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.getId() + "false")
            else -> throw Exception("Test failed - LogType was $lt")
        }
        driver.findElement(By.id(id)).click()
    }

    fun save() {
        driver.findElement(By.id(LoggingAPI.Elements.SAVE_BUTTON.getId())).click()
    }

    fun isLoggingOn(lt : LogTypes) : Boolean {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.getId() + "true")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.getId() + "true")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.getId() + "true")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.getId() + "true")
            else -> throw Exception("Test failed - LogType was $lt")
        }
        return driver.findElement(By.id(id)).getAttribute("checked") == "true"
    }

}

class LogoutPage(private val driver: WebDriver, private val domain: String) {

    fun go() {
        driver.get("$domain/${LogoutAPI.path}")
    }

}

class ViewTimePage(private val driver: WebDriver, private val domain: String) {

    fun getTimeForEntry(id: Int) : String {
        return driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.TIME_INPUT.getElemName()}]")).getAttribute("value")
    }

    fun getDateForEntry(id: Int) : String {
        return driver.findElement(By.cssSelector("#time-entry-$id input[name=${ViewTimeAPI.Elements.DATE_INPUT.getElemName()}]")).getAttribute("value")
    }
}
