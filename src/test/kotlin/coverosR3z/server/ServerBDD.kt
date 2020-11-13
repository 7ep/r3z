package coverosR3z.server

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import coverosR3z.main
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver


/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class ServerBDD {

    companion object {
        @BeforeClass @JvmStatic
        fun setUp() {
            // start the server
            Thread{main()}.start()
        }
    }

    /*
     *  Here we are testing that a common browser, Chrome, is usable
     *  for accessing the application.  We will also test on a simpler
     *  headless browser without javascript, in order to make sure we
     *  remain "graceful degradation". This also helps keep up mobile-first,
     *  easily accessible, and highly performant.
     */
    @Test
    fun `happy path - I should be able to see a page - Chrome`() {
        // Given I am a Chrome browser user
        // Start selenium
        val driver = ChromeDriver()

        // When I go to the homepage
        driver.get("localhost:8080/homepage")

        // Then I see it successfully in the browser
        assertEquals("Homepage", driver.title)
        driver.quit()
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
    fun `Smoke test - traverse through application, many pitstops`() {
        // Given I am a Chrome browser user
        // Start selenium
        val driver = ChromeDriver()

        // Hit the homepage
        driver.get("localhost:8080/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", driver.title)

        // register
        driver.get("localhost:8080/${NamedPaths.REGISTER.path}")
        val user = "user1"
        driver.findElementById("username").sendKeys(user)
        val password = "password123456"
        driver.findElementById("password").sendKeys(password)
        driver.findElement(By.id("employee")).findElement(By.xpath("//option[. = 'alice']")).click()
        driver.findElementById("register_button").click()

        // login
        driver.get("localhost:8080/${NamedPaths.LOGIN.path}")
        driver.findElementById("username").sendKeys(user)
        driver.findElementById("password").sendKeys(password)
        driver.findElementById("login_button").click()

        // hit authenticated homepage
        driver.get("localhost:8080/${NamedPaths.AUTHHOMEPAGE.path}")
        assertEquals("Authenticated Homepage", driver.title)

        // hit the 404 error
        driver.get("localhost:8080/idontexist")
        assertEquals("404 error", driver.title)

        // enter a new project
        driver.get("localhost:8080/${NamedPaths.CREATE_PROJECT.path}")
        val projecta = "projecta"
        driver.findElementById("project_name").sendKeys(projecta)
        driver.findElementById("project_create_button").click()

        // enter a new employee
        driver.get("localhost:8080/${NamedPaths.CREATE_EMPLOYEE.path}")
        driver.findElementById("employee_name").sendKeys(user)
        driver.findElementById("employee_create_button").click()

        // enter time
        driver.get("localhost:8080/${NamedPaths.ENTER_TIME.path}")
        driver.findElementById("project_entry").sendKeys("1")
        driver.findElementById("time_entry").sendKeys("120")
        driver.findElementById("detail_entry").sendKeys("work on the lab")
        driver.findElementById("enter_time_button").click()

        driver.quit()
    }

    /**
     * HtmlUnit is a Java-based headless browser
     */
    @Test
    fun `happy path - I should be able to see a page without javascript`() {
        // Given I am on a browser without javascript
        // start the HtmlUnit headless browser
        val driver = WebClient()
        // prevent javascript from running.  We want these tests to really zip.
        driver.options.isJavaScriptEnabled = false

        // When I go to the homepage
        val page : HtmlPage = driver.getPage("http://localhost:8080/homepage")
        // Then I see it successfully in the browser
        val titleText = page.titleText

        assertEquals("Homepage", titleText)
        driver.close()
    }

}