package coverosR3z.server

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import java.net.SocketException


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

        // register
        chromeDriver.get("localhost:8080/${NamedPaths.REGISTER.path}")
        val user = "user1"
        chromeDriver.findElementById("username").sendKeys(user)
        val password = "password123456"
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = 'alice']")).click()
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

        // enter a new project
        chromeDriver.get("localhost:8080/${NamedPaths.CREATE_PROJECT.path}")
        val projecta = "projecta"
        chromeDriver.findElementById("project_name").sendKeys(projecta)
        chromeDriver.findElementById("project_create_button").click()

        // enter a new employee
        chromeDriver.get("localhost:8080/${NamedPaths.CREATE_EMPLOYEE.path}")
        chromeDriver.findElementById("employee_name").sendKeys(user)
        chromeDriver.findElementById("employee_create_button").click()

        // enter time
        chromeDriver.get("localhost:8080/${NamedPaths.ENTER_TIME.path}")
        // projecta's id is 1, we have to pass that.
        chromeDriver.findElementById("project_entry").sendKeys("1")
        chromeDriver.findElementById("time_entry").sendKeys("120")
        chromeDriver.findElementById("detail_entry").sendKeys("work on the lab")
        chromeDriver.findElementById("enter_time_button").click()
        chromeDriver.quit()
    }

}