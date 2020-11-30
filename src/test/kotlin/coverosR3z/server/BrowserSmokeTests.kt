package coverosR3z.server

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.EnterTimeElements
import coverosR3z.authentication.LoginElements
import coverosR3z.timerecording.EmployeeElements
import coverosR3z.timerecording.ProjectElements
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import java.io.File


private const val domain = "http://localhost:8080"

/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class BrowserSmokeTests {

    companion object {
        private lateinit var sc : SocketCommunication
        private lateinit var serverThread : Thread
        const val dbDirectory = "build/db/"

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()

            // wipe out the database
            File(dbDirectory).deleteRecursively()

            // start the server
            serverThread = Thread {
                sc = SocketCommunication(8080, dbDirectory)
                sc.startServer()
            }
            serverThread.start()
        }

    }

    /**
     * HtmlUnit is a Java-based headless browser
     */
    @Test
    fun `happy path - I should be able to see a page without javascript`() {
        // Given I am on a browser without javascript
        // start the HTMLUnit browser
        val htmlUnitDriver = WebClient()
        // prevent javascript from running.  We want these tests to really zip.
        htmlUnitDriver.options.isJavaScriptEnabled = false

        // When I go to the homepage
        val page : HtmlPage = htmlUnitDriver.getPage("$domain/homepage")
        // Then I see it successfully in the browser
        val titleText = page.titleText

        assertEquals("Homepage", titleText)
        htmlUnitDriver.close()
    }

    /**
     * In this system-wide smoke test we will exercise many parts of the application.
     * We will:
     * - register a user and login
     * - register an employee
     * - create a project
     * - hit the authenticated homepage
     * - enter time
     */
    @Test
    fun `Smoke test - traverse through application with Chrome, many pitstops`() {
        // Given I am a Chrome browser user
        // start the Chromedriver
        val chromeDriver = ChromeDriver()
        val rp = RegisterPage(chromeDriver)
        val lp = LoginPage(chromeDriver)
        val etp = EnterTimePage(chromeDriver)
        val eep = EnterEmployeePage(chromeDriver)
        val epp = EnterProjectPage(chromeDriver)
        val user = "Henry the Eighth I am I am, Henry the Eighth I am!"
        val password = "l!Mfr~Wc9gIz'pbXs7[]l|'lBM4/Ng3t8nYevRUNQcL_+SW%A522sThETaQlbB^{qiNJWzpblP`24N_V8A6#A-2T#4}c)DP%;m1WC_RXlI}MyZHo7*Q1(kC+lC/9('+jMA9/fr\$IZ,\\5=BivXp36tb"
        val details = "!\"#\$%&'()*+,-./A0123456789A:;<=>?@UABCDEFGHIJKLMNOPQRSTUVWXYZA[\\]^_`LabcdefghijklmnopqrstuvwxyzA{|}~CL¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖM×LØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöM÷LøùúûüýþÿEŁłŃńŅņŇňEŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŴŵŶŷŸŹźŻżŽžſ"

        // Hit the homepage
        chromeDriver.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", chromeDriver.title)

        // Hit the current commit page
        chromeDriver.get("$domain/commit.html")

        rp.register(user, password, "Administrator")
        lp.login(user, password)

        // hit authenticated homepage
        chromeDriver.get("$domain/${NamedPaths.AUTHHOMEPAGE.path}")
        assertEquals("Authenticated Homepage", chromeDriver.title)

        // hit the 404 error
        chromeDriver.get("$domain/idontexist")
        assertEquals("404 error", chromeDriver.title)

        // enter a few new projects
        val projects = listOf("BDD", "FTA")
        for (project in projects) {
            epp.enter(project)
        }

        // enter a new employee
        val employees = listOf(DEFAULT_USER.name.value, "<script>alert()</script>")
        for (employee in employees) {
            eep.enter(employee)
        }

        // view the employees
        chromeDriver.get("$domain/${NamedPaths.EMPLOYEES.path}")
        assertEquals("Company Employees", chromeDriver.title)
        assertTrue(chromeDriver.findElementsByTagName("td").any{ it.text!!.contentEquals("Administrator")})
        assertTrue(chromeDriver.findElementsByTagName("td").any{ it.text!!.contentEquals("DefaultUser")})

        // logout
        chromeDriver.get("$domain/logout")

        // register a user for each employee
        for (e in employees) {
            rp.register(e, password, e)
        }

        // loop through each user and login in
        for (e in employees) {
            // login
            lp.login(e, password)

            // enter times
            for (p in projects) {
                etp.enterTime(p, "60", details)
            }

            // logout
            chromeDriver.get("$domain/logout")
            assertEquals("Logout", chromeDriver.title)
        }

        // login
        lp.login(user, password)

        // view the time entries for the last person
        chromeDriver.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", chromeDriver.title)

        // shut the server down
        chromeDriver.get("$domain/${NamedPaths.SHUTDOWN_SERVER.path}")
        chromeDriver.get("$domain/${NamedPaths.SHUTDOWN_SERVER.path}")

        // close Chrome entirely
        chromeDriver.quit()

        // start the server
        serverThread.join()
        Thread {
            SocketCommunication.shouldContinue = true
            sc = SocketCommunication(8080, dbDirectory)
            sc.startServer()
        }.start()

        // restart Chrome
        val chromedriver2 = ChromeDriver()
        val lp2 = LoginPage(chromedriver2)
        val etp2 = EnterTimePage(chromedriver2)

        // Hit the homepage
        chromedriver2.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", chromedriver2.title)

        // loop through each user and login in
        for (e in employees) {
            // login
            lp2.login(e, password)

            // enter times
            for (p in projects) {
                etp2.enterTime(p, "60", "foo foo foo foo la la la la la la")
            }

            // logout
            chromedriver2.get("$domain/logout")
        }

        // login
        lp2.login(employees[0], password)

        // view the time entries for the last person
        chromedriver2.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", chromedriver2.title)
        val allEntries = chromedriver2.findElementByTagName("table").text
        assertTrue(allEntries.contains(details))

        // shut the server down
        chromedriver2.get("$domain/${NamedPaths.SHUTDOWN_SERVER.path}")

        chromedriver2.quit()
    }

    private class EnterTimePage(private val driver : ChromeDriver) {

        fun enterTime(project : String, time : String, details : String) {
            driver.get("$domain/${NamedPaths.ENTER_TIME.path}")
            driver.findElement(By.id(EnterTimeElements.PROJECT_INPUT.id)).findElement(By.xpath("//option[. = '$project']")).click()
            driver.findElementById(EnterTimeElements.TIME_INPUT.id).sendKeys(time)
            driver.findElementById(EnterTimeElements.DETAIL_INPUT.id).sendKeys(details)
            driver.findElementById(EnterTimeElements.ENTER_TIME_BUTTON.id).click()
            assertEquals("SUCCESS", driver.title)
        }
    }
    private class LoginPage(private val driver : ChromeDriver) {
        
        fun login(username : String, password : String) {
            driver.get("$domain/${NamedPaths.LOGIN.path}")
            driver.findElementById(LoginElements.USERNAME_INPUT.id).sendKeys(username)
            driver.findElementById(LoginElements.PASSWORD_INPUT.id).sendKeys(password)
            driver.findElementById(LoginElements.LOGIN_BUTTON.id).click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class RegisterPage(private val driver : ChromeDriver) {

        fun register(username: String, password : String, employee : String) {
            driver.get("$domain/${NamedPaths.REGISTER.path}")
            driver.findElementById("username").sendKeys(username)
            driver.findElementById("password").sendKeys(password)
            driver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '$employee']")).click()
            driver.findElementById("register_button").click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class EnterEmployeePage(private val driver : ChromeDriver) {

        fun enter(employee: String) {
            driver.get("$domain/${NamedPaths.CREATE_EMPLOYEE.path}")
            driver.findElementById(EmployeeElements.EMPLOYEE_INPUT.id).sendKeys(employee)
            driver.findElementById(EmployeeElements.CREATE_BUTTON.id).click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class EnterProjectPage(private val driver : ChromeDriver) {

        fun enter(project: String) {
            driver.get("$domain/${NamedPaths.CREATE_PROJECT.path}")
            driver.findElementById(ProjectElements.PROJECT_INPUT.id).sendKeys(project)
            driver.findElementById(ProjectElements.CREATE_BUTTON.id).click()
            assertEquals("SUCCESS", driver.title)
        }
    }


}