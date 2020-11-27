package coverosR3z.server

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import coverosR3z.DEFAULT_USER
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import java.io.File


/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class BrowserBDD {

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

        @AfterClass
        @JvmStatic
        fun cleanup() {
            // wipe out the database
            File(dbDirectory).deleteRecursively()
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
    @Test
    fun `Smoke test - traverse through application with Chrome, many pitstops`() {
        // Given I am a Chrome browser user
        // start the Chromedriver
        val chromeDriver = ChromeDriver()

        // Hit the homepage
        chromeDriver.get("localhost:8080/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", chromeDriver.title)

        // Hit the current commit page
        chromeDriver.get("localhost:8080/commit.html")

        // register
        chromeDriver.get("localhost:8080/${NamedPaths.REGISTER.path}")
        val user = "user1"
        chromeDriver.findElementById("username").sendKeys(user)
        val password = "password123456"
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = 'Administrator']")).click()
        chromeDriver.findElementById("register_button").click()
        assertEquals("SUCCESS", chromeDriver.title)

        // login
        chromeDriver.get("localhost:8080/${NamedPaths.LOGIN.path}")
        chromeDriver.findElementById("username").sendKeys(user)
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElementById("login_button").click()
        assertEquals("SUCCESS", chromeDriver.title)

        // hit authenticated homepage
        chromeDriver.get("localhost:8080/${NamedPaths.AUTHHOMEPAGE.path}")
        assertEquals("Authenticated Homepage", chromeDriver.title)

        // hit the 404 error
        chromeDriver.get("localhost:8080/idontexist")
        assertEquals("404 error", chromeDriver.title)

        // enter a few new projects
        val projects = listOf("BDD", "FTA")
        for (project in projects) {
            chromeDriver.get("localhost:8080/${NamedPaths.CREATE_PROJECT.path}")
            chromeDriver.findElementById("project_name").sendKeys(project)
            chromeDriver.findElementById("project_create_button").click()
            assertEquals("SUCCESS", chromeDriver.title)
        }

        // enter a new employee
        val employees = listOf(DEFAULT_USER.name.value)
        for (employee in employees) {
            chromeDriver.get("localhost:8080/${NamedPaths.CREATE_EMPLOYEE.path}")
            chromeDriver.findElementById("employee_name").sendKeys(employee)
            chromeDriver.findElementById("employee_create_button").click()
            assertEquals("SUCCESS", chromeDriver.title)
        }

        // view the employees
        chromeDriver.get("localhost:8080/${NamedPaths.EMPLOYEES.path}")
        assertEquals("Company Employees", chromeDriver.title)
        assertTrue(chromeDriver.findElementsByTagName("td").any{ it.text!!.contentEquals("Administrator")})
        assertTrue(chromeDriver.findElementsByTagName("td").any{ it.text!!.contentEquals("DefaultUser")})

        // logout
        chromeDriver.get("localhost:8080/logout")

        // register a user for each employee
        for (e in employees) {
            chromeDriver.get("localhost:8080/${NamedPaths.REGISTER.path}")
            chromeDriver.findElementById("username").sendKeys(e)
            chromeDriver.findElementById("password").sendKeys(password)
            chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '${e}']")).click()
            chromeDriver.findElementById("register_button").click()
            assertEquals("SUCCESS", chromeDriver.title)
        }

        // loop through each user and login in
        for (e in employees) {
            // login
            chromeDriver.get("localhost:8080/${NamedPaths.LOGIN.path}")
            chromeDriver.findElementById("username").sendKeys(e)
            chromeDriver.findElementById("password").sendKeys(password)
            chromeDriver.findElementById("login_button").click()
            assertEquals("SUCCESS", chromeDriver.title)

            // enter times
            for (p in projects) {
                chromeDriver.get("localhost:8080/${NamedPaths.ENTER_TIME.path}")
                chromeDriver.findElement(By.id("project_entry")).findElement(By.xpath("//option[. = '$p']")).click()
                chromeDriver.findElementById("time_entry").sendKeys("60")
                chromeDriver.findElementById("detail_entry").sendKeys("foo foo foo foo la la la la la la")
                chromeDriver.findElementById("enter_time_button").click()
                assertEquals("SUCCESS", chromeDriver.title)
            }

            // logout
            chromeDriver.get("localhost:8080/logout")
            assertEquals("Logout", chromeDriver.title)
        }

        // login
        chromeDriver.get("localhost:8080/${NamedPaths.LOGIN.path}")
        chromeDriver.findElementById("username").sendKeys(user)
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElementById("login_button").click()
        assertEquals("SUCCESS", chromeDriver.title)

        // view the time entries for the last person
        chromeDriver.get("localhost:8080/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", chromeDriver.title)

        // shut the server down
        chromeDriver.get("localhost:8080/${NamedPaths.SHUTDOWN_SERVER.path}")
        chromeDriver.get("localhost:8080/${NamedPaths.SHUTDOWN_SERVER.path}")

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
        // Hit the homepage
        chromedriver2.get("localhost:8080/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", chromedriver2.title)

        // loop through each user and login in
        for (e in employees) {
            // login
            chromedriver2.get("localhost:8080/${NamedPaths.LOGIN.path}")
            chromedriver2.findElementById("username").sendKeys(e)
            chromedriver2.findElementById("password").sendKeys(password)
            chromedriver2.findElementById("login_button").click()
            assertEquals("SUCCESS", chromedriver2.title)

            // enter times
            for (p in projects) {
                chromedriver2.get("localhost:8080/${NamedPaths.ENTER_TIME.path}")
                chromedriver2.findElement(By.id("project_entry")).findElement(By.xpath("//option[. = '$p']")).click()
                chromedriver2.findElementById("time_entry").sendKeys("60")
                chromedriver2.findElementById("detail_entry").sendKeys("foo foo foo foo la la la la la la")
                chromedriver2.findElementById("enter_time_button").click()
                assertEquals("SUCCESS", chromedriver2.title)
            }

            // logout
            chromedriver2.get("localhost:8080/logout")
        }

        // login
        chromedriver2.get("localhost:8080/${NamedPaths.LOGIN.path}")
        chromedriver2.findElementById("username").sendKeys(employees[0])
        chromedriver2.findElementById("password").sendKeys(password)
        chromedriver2.findElementById("login_button").click()
        assertEquals("SUCCESS", chromedriver2.title)

        // view the time entries for the last person
        chromedriver2.get("localhost:8080/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", chromedriver2.title)
        val allEntries = chromedriver2.findElementByTagName("table").text
        assertTrue(allEntries.contains("BDD 60 foo foo foo foo la la la la la la"))

        // shut the server down
        chromedriver2.get("localhost:8080/${NamedPaths.SHUTDOWN_SERVER.path}")

        chromedriver2.quit()
    }

}