package coverosR3z.uitests

import com.gargoylesoftware.htmlunit.BrowserVersion
import coverosR3z.authentication.LoginAPI
import coverosR3z.logging.LogTypes
import coverosR3z.logging.LoggingAPI
import coverosR3z.server.NamedPaths
import coverosR3z.timerecording.EmployeeAPI
import coverosR3z.timerecording.EnterTimeAPI
import coverosR3z.timerecording.ProjectAPI
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver

enum class Drivers(val driver: () -> WebDriver){
    HTMLUNIT({ HtmlUnitDriver(BrowserVersion.BEST_SUPPORTED) }),
    FIREFOX({ FirefoxDriver() }),
    CHROME({ ChromeDriver(ChromeOptions().setHeadless(false)) })
}

class EnterTimePage(private val driver: WebDriver, val domain : String) {

    fun enterTime(project: String, time: String, details: String, date: String) {
        driver.get("$domain/${NamedPaths.ENTER_TIME.path}")
        driver.findElement(By.id(EnterTimeAPI.Elements.PROJECT_INPUT.id)).findElement(By.xpath("//option[. = '$project']")).click()
        driver.findElement(By.id(EnterTimeAPI.Elements.TIME_INPUT.id)).sendKeys(time)
        driver.findElement(By.id(EnterTimeAPI.Elements.DETAIL_INPUT.id)).sendKeys(details)
        driver.findElement(By.id(EnterTimeAPI.Elements.DATE_INPUT.id)).sendKeys(date)
        driver.findElement(By.id(EnterTimeAPI.Elements.ENTER_TIME_BUTTON.id)).click()
    }
}

class LoginPage(private val driver: WebDriver, val domain : String) {

    fun login(username: String, password: String) {
        driver.get("$domain/${NamedPaths.LOGIN.path}")
        driver.findElement(By.id(LoginAPI.Elements.USERNAME_INPUT.id)).sendKeys(username)
        driver.findElement(By.id(LoginAPI.Elements.PASSWORD_INPUT.id)).sendKeys(password)
        driver.findElement(By.id(LoginAPI.Elements.LOGIN_BUTTON.id)).click()
    }
}

class RegisterPage(private val driver: WebDriver, val domain : String) {

    fun register(username: String, password: String, employee: String) {
        driver.get("$domain/${NamedPaths.REGISTER.path}")
        driver.findElement(By.id("username")).sendKeys(username)
        driver.findElement(By.id("password")).sendKeys(password)
        driver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '$employee']")).click()
        driver.findElement(By.id("register_button")).click()
    }
}

class EnterEmployeePage(private val driver: WebDriver, val domain : String) {

    fun enter(employee: String) {
        driver.get("$domain/${NamedPaths.CREATE_EMPLOYEE.path}")
        driver.findElement(By.id(EmployeeAPI.Elements.EMPLOYEE_INPUT.id)).sendKeys(employee)
        driver.findElement(By.id(EmployeeAPI.Elements.CREATE_BUTTON.id)).click()
    }
}

class EnterProjectPage(private val driver: WebDriver, val domain : String) {

    fun enter(project: String) {
        driver.get("$domain/${NamedPaths.CREATE_PROJECT.path}")
        driver.findElement(By.id(ProjectAPI.Elements.PROJECT_INPUT.id)).sendKeys(project)
        driver.findElement(By.id(ProjectAPI.Elements.CREATE_BUTTON.id)).click()
    }
}

class LoggingPage(private val driver: WebDriver, private val domain: String) {

    fun go() {
        driver.get("$domain/${NamedPaths.LOGGING.path}")
    }

    fun setLoggingTrue(lt : LogTypes) {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.id + "true")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.id + "true")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.id + "true")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.id + "true")
            else -> throw Exception("Test failed - LogType was $lt")
        }
        driver.findElement(By.id(id)).click()
    }

    fun setLoggingFalse(lt : LogTypes) {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.id + "false")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.id + "false")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.id + "false")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.id + "false")
            else -> throw Exception("Test failed - LogType was $lt")
        }
        driver.findElement(By.id(id)).click()
    }

    fun save() {
        driver.findElement(By.id(LoggingAPI.Elements.SAVE_BUTTON.id)).click()
    }

    fun isLoggingOn(lt : LogTypes) : Boolean {
        val id = when (lt) {
            LogTypes.AUDIT -> (LoggingAPI.Elements.AUDIT_INPUT.id + "true")
            LogTypes.WARN -> (LoggingAPI.Elements.WARN_INPUT.id + "true")
            LogTypes.DEBUG -> (LoggingAPI.Elements.DEBUG_INPUT.id + "true")
            LogTypes.TRACE -> (LoggingAPI.Elements.TRACE_INPUT.id + "true")
            else -> throw Exception("Test failed - LogType was $lt")
        }
        return driver.findElement(By.id(id)).getAttribute("checked") == "true"
    }

}

class LogoutPage(private val driver: WebDriver, private val domain: String) {

    fun go() {
        driver.get("$domain/${NamedPaths.LOGOUT.path}")
    }

}