package coverosR3z.uitests

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver


/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class UITests {

    private val server = "localhost"
    private val port = 8080


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
        val page : HtmlPage = htmlUnitDriver.getPage("http://$server:$port/homepage")
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
        chromeDriver.get("$server:$port/homepage")
        assertEquals("Homepage", chromeDriver.title)

        // register
        chromeDriver.get("$server:$port/register")
        val user = "user1"
        chromeDriver.findElementById("username").sendKeys(user)
        val password = "password123456"
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = 'Administrator']")).click()
        chromeDriver.findElementById("register_button").click()

        // login
        chromeDriver.get("$server:$port/login")
        chromeDriver.findElementById("username").sendKeys(user)
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElementById("login_button").click()

        // hit authenticated homepage
        chromeDriver.get("$server:$port/homepage")
        assertEquals("Authenticated Homepage", chromeDriver.title)

        // hit the 404 error
        chromeDriver.get("$server:$port/idontexist")
        assertEquals("404 error", chromeDriver.title)

        // enter a few new projects
        val projects = listOf("BDD", "FTA", "STF", "EAW", "TDD")
        for (project in projects) {
            chromeDriver.get("$server:$port/createproject")
            chromeDriver.findElementById("project_name").sendKeys(project)
            chromeDriver.findElementById("project_create_button").click()
        }

        // enter a new employee
        val employees = listOf("alice", "bob", "carol", "david", "edward")
        for (employee in employees) {
            chromeDriver.get("$server:$port/createemployee")
            chromeDriver.findElementById("employee_name").sendKeys(employee)
            chromeDriver.findElementById("employee_create_button").click()
        }

        // view the employees
        chromeDriver.get("$server:$port/employees")

        // logout
        chromeDriver.get("$server:$port/logout")

        // register a user for each employee
        for (e in employees) {
            chromeDriver.get("$server:$port/register")
            chromeDriver.findElementById("username").sendKeys(e)
            chromeDriver.findElementById("password").sendKeys(password)
            chromeDriver.findElement(By.id("employee")).findElement(By.xpath("//option[. = '${e}']")).click()
            chromeDriver.findElementById("register_button").click()
        }

        // loop through each user and login in
        for (e in employees) {
            // login
            chromeDriver.get("$server:$port/login")
            chromeDriver.findElementById("username").sendKeys(e)
            chromeDriver.findElementById("password").sendKeys(password)
            chromeDriver.findElementById("login_button").click()

            // enter times
            for (p in projects) {
                chromeDriver.get("$server:$port/entertime")
                chromeDriver.findElement(By.id("project_entry")).findElement(By.xpath("//option[. = '$p']")).click()
                chromeDriver.findElementById("time_entry").sendKeys("60")
                chromeDriver.findElementById("detail_entry").sendKeys("foo foo foo foo la la la la la la")
                chromeDriver.findElementById("enter_time_button").click()
            }

            // logout
            chromeDriver.get("$server:$port/logout")
        }

        // login
        chromeDriver.get("$server:$port/login")
        chromeDriver.findElementById("username").sendKeys(user)
        chromeDriver.findElementById("password").sendKeys(password)
        chromeDriver.findElementById("login_button").click()

        // view the time entries for the last person
        chromeDriver.get("$server:$port/timeentries")

        chromeDriver.quit()
    }

}