package coverosR3z.server

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver


/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class BrowserBDD {

    companion object {
        lateinit var sc : SocketCommunication
        lateinit var serverThread : Thread

        @BeforeClass @JvmStatic
        fun setup() {
            // start the server
            serverThread = Thread{
                    sc = SocketCommunication(8080)
                    sc.startServer()
            }
            serverThread.start()
        }

        @AfterClass @JvmStatic
        fun cleanup() {
            // stop the server
            sc.halfOpenServerSocket.close()
            serverThread.join()
        }
    }

    /**
     * HtmlUnit is a Java-based headless browser
     */
    @Ignore("This is manually run as a development testing tool")
    @Test
    fun `happy path - I should be able to see a page without javascript`() {
        // Given I am on a browser without javascript
        // start the HTMLUnit browser
        val htmlUnitDriver = WebClient()
        // prevent javascript from running.  We want these tests to really zip.
        htmlUnitDriver.options.isJavaScriptEnabled = false

        // When I go to the homepage
        val page : HtmlPage = htmlUnitDriver.getPage("http://localhost:8080/homepage")
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
//    @Ignore("This is manually run as a development testing tool")
    @Test
    fun `Smoke test - traverse through application with Chrome, many pitstops`() {
        // Given I am a Chrome browser user
        // start the Chromedriver
        val chromeDriver = ChromeDriver()

        // Hit the homepage
        chromeDriver.get("localhost:8080/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", chromeDriver.title)

        // register
        chromeDriver.get("localhost:8080/${NamedPaths.REGISTER.path}")
        val user = "user1"
        chromeDriver.findElementById("username").sendKeys(user)
        val password = "password123456"
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = 'Administrator']")).click()
        chromeDriver.findElementById("register_button").click()

        // login
        chromeDriver.get("localhost:8080/${NamedPaths.LOGIN.path}")
        chromeDriver.findElementById("username").sendKeys(user)
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElementById("login_button").click()

        // hit authenticated homepage
        chromeDriver.get("localhost:8080/${NamedPaths.AUTHHOMEPAGE.path}")
        assertEquals("Authenticated Homepage", chromeDriver.title)

        // hit the 404 error
        chromeDriver.get("localhost:8080/idontexist")
        assertEquals("404 error", chromeDriver.title)

        // enter a few new projects
        val projects = listOf("BDD", "FTA", "STF", "TaxAct", "Vibrent")
        for (project in projects) {
            chromeDriver.get("localhost:8080/${NamedPaths.CREATE_PROJECT.path}")
            chromeDriver.findElementById("project_name").sendKeys(project)
            chromeDriver.findElementById("project_create_button").click()
        }

        // enter a new employee
        val employees = listOf("matt", "mitch", "byron", "jenna", "jona")
        for (employee in employees) {
            chromeDriver.get("localhost:8080/${NamedPaths.CREATE_EMPLOYEE.path}")
            chromeDriver.findElementById("employee_name").sendKeys(employee)
            chromeDriver.findElementById("employee_create_button").click()
        }

        // view the employees
        chromeDriver.get("localhost:8080/${NamedPaths.EMPLOYEES.path}")

        // logout
        chromeDriver.get("localhost:8080/logout")

        // register a user for each employee
        for (e in employees) {
            chromeDriver.get("localhost:8080/${NamedPaths.REGISTER.path}")
            chromeDriver.findElementById("username").sendKeys(e)
            chromeDriver.findElementById("password").sendKeys(password)
            chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '$e']")).click()
            chromeDriver.findElementById("register_button").click()
        }

        // loop through each user and login in
        for (e in employees) {
            // login
            chromeDriver.get("localhost:8080/${NamedPaths.LOGIN.path}")
            chromeDriver.findElementById("username").sendKeys(e)
            chromeDriver.findElementById("password").sendKeys(password)
            chromeDriver.findElementById("login_button").click()

            // enter times
            for (p in projects) {
                chromeDriver.get("localhost:8080/${NamedPaths.ENTER_TIME.path}")
                chromeDriver.findElement(By.id("project_entry")).findElement(By.xpath("//option[. = '$p']")).click()
                chromeDriver.findElementById("time_entry").sendKeys("60")
                chromeDriver.findElementById("detail_entry").sendKeys("foo foo foo foo la la la la la la")
                chromeDriver.findElementById("enter_time_button").click()
            }

            // logout
            chromeDriver.get("localhost:8080/logout")
        }

        // login
        chromeDriver.get("localhost:8080/${NamedPaths.LOGIN.path}")
        chromeDriver.findElementById("username").sendKeys(user)
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElementById("login_button").click()

        // view the time entries for the last person
        chromeDriver.get("localhost:8080/${NamedPaths.TIMEENTRIES.path}")

        chromeDriver.quit()
    }

}