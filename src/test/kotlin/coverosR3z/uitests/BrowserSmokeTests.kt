package coverosR3z.uitests

import com.gargoylesoftware.htmlunit.BrowserVersion.BEST_SUPPORTED
import coverosR3z.DEFAULT_USER
import coverosR3z.timerecording.EnterTimeElements
import coverosR3z.authentication.LoginElements
import coverosR3z.logging.LogTypes
import coverosR3z.logging.logSettings
import coverosR3z.server.NamedPaths
import coverosR3z.server.Server
import coverosR3z.timerecording.EmployeeElements
import coverosR3z.timerecording.ProjectElements
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


private const val domain = "http://localhost:8080"

/**
 * As a user of r3z
 * I want to access it through a browser
 * So that it is convenient
 */
class BrowserSmokeTests {

    companion object {
        private lateinit var sc : Server
        private lateinit var serverThread : Thread
        const val dbDirectory = "build/db/"

        @BeforeClass
        @JvmStatic
        fun setup() {
            logSettings[LogTypes.DEBUG] = true
            logSettings[LogTypes.TRACE] = true
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    @Before
    fun init() {
        // wipe out the database
        File(dbDirectory).deleteRecursively()

        // start the server
        serverThread = Thread {
            sc = Server(8080, dbDirectory)
            sc.startServer()
        }
        serverThread.start()
    }

    @After
    fun clean() {
        sc.halfOpenServerSocket.close()
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
    @Ignore
    @Test
    fun `Smoke test - traverse through application with Chrome, many pitstops`() {
        // Given I am a Chrome browser user
        // start the Chromedriver
        val driver = ChromeDriver(ChromeOptions().setHeadless(false))
        bigSmokeTest(driver)
    }

    @Test
    fun `Smoke test - with htmlunit`() {
        // start the HtmlUnitDriver
        val driver = HtmlUnitDriver(BEST_SUPPORTED)
        bigSmokeTest(driver)
    }

    @Ignore
    @Test
    fun `Smoke test - with firefox`() {
        // start the Firefox driver
        val driver = FirefoxDriver()
        bigSmokeTest(driver)
    }

    private fun bigSmokeTest(driver: WebDriver) {
        val rp = RegisterPage(driver)
        val lp = LoginPage(driver)
        val etp = EnterTimePage(driver)
        val eep = EnterEmployeePage(driver)
        val epp = EnterProjectPage(driver)
        val user = "Henry the Eighth I am I am, Henry the Eighth I am!"
        val password = "l!Mfr~Wc9gIz'pbXs7[]l|'lBM4/Ng3t8nYevRUNQcL_+SW%A522sThETaQlbB^{qiNJWzpblP`24N_V8A6#A-2T#4}c)DP%;m1WC_RXlI}MyZHo7*Q1(kC+lC/9('+jMA9/fr\$IZ,\\5=BivXp36tb"
        val details = "!\"#\$%&'()*+,-./A0123456789A:;<=>?@UABCDEFGHIJKLMNOPQRSTUVWXYZA[\\]^_`LabcdefghijklmnopqrstuvwxyzA{|}~CL¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿LÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖM×LØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöM÷LøùúûüýþÿEŁłŃńŅņŇňEŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŴŵŶŷŸŹźŻżŽžſ"

        // Hit the homepage
        driver.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", driver.title)

        // Hit the current commit page
        driver.get("$domain/commit.html")

        rp.register(user, password, "Administrator")
        lp.login(user, password)

        // hit authenticated homepage
        driver.get("$domain/${NamedPaths.AUTHHOMEPAGE.path}")
        assertEquals("Authenticated Homepage", driver.title)

        // hit the 404 error
        driver.get("$domain/idontexist")
        assertEquals("404 error", driver.title)

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
        driver.get("$domain/${NamedPaths.EMPLOYEES.path}")
        assertEquals("Company Employees", driver.title)
        assertTrue(driver.findElements(By.tagName("td")).any { it.text!!.contentEquals("Administrator") })
        assertTrue(driver.findElements(By.tagName("td")).any { it.text!!.contentEquals("DefaultUser") })

        // logout
        driver.get("$domain/logout")

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
            driver.get("$domain/logout")
            assertEquals("Logout", driver.title)
        }

        // login
        lp.login(user, password)

        // view the time entries for the last person
        driver.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", driver.title)

        // Hit the homepage
        driver.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Authenticated Homepage", driver.title)

        // logout
        driver.get("$domain/${NamedPaths.LOGOUT.path}")

        // loop through each user and login in
        for (e in employees) {
            // login
            lp.login(e, password)

            // enter times
            for (p in projects) {
                etp.enterTime(p, "60", "foo foo foo foo la la la la la la")
            }

            // logout
            driver.get("$domain/logout")
        }

        // login
        lp.login(employees[0], password)

        // view the time entries for the last person
        driver.get("$domain/${NamedPaths.TIMEENTRIES.path}")
        assertEquals("your time entries", driver.title)
        val allEntries = driver.findElement(By.ByTagName("table")).text
        assertTrue(allEntries.contains(details))

        // add a new employee
        val newEmployeeName = "some new employee"
        eep.enter(newEmployeeName)

        // Hit the homepage
        driver.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Authenticated Homepage", driver.title)

        // logout
        driver.get("$domain/${NamedPaths.LOGOUT.path}")

        // register as the new employee
        rp.register("newuser", password, newEmployeeName)

        // login as the new employee
        lp.login("newuser", password)

        driver.quit()
    }

    private class EnterTimePage(private val driver: WebDriver) {

        fun enterTime(project: String, time: String, details: String) {
            driver.get("$domain/${NamedPaths.ENTER_TIME.path}")
            driver.findElement(By.id(EnterTimeElements.PROJECT_INPUT.id)).findElement(By.xpath("//option[. = '$project']")).click()
            driver.findElement(By.id(EnterTimeElements.TIME_INPUT.id)).sendKeys(time)
            driver.findElement(By.id(EnterTimeElements.DETAIL_INPUT.id)).sendKeys(details)
            driver.findElement(By.id(EnterTimeElements.ENTER_TIME_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }
    private class LoginPage(private val driver: WebDriver) {
        
        fun login(username: String, password: String) {
            driver.get("$domain/${NamedPaths.LOGIN.path}")
            driver.findElement(By.id(LoginElements.USERNAME_INPUT.id)).sendKeys(username)
            driver.findElement(By.id(LoginElements.PASSWORD_INPUT.id)).sendKeys(password)
            driver.findElement(By.id(LoginElements.LOGIN_BUTTON.id)).click()
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
            driver.findElement(By.id(EmployeeElements.EMPLOYEE_INPUT.id)).sendKeys(employee)
            driver.findElement(By.id(EmployeeElements.CREATE_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }

    private class EnterProjectPage(private val driver: WebDriver) {

        fun enter(project: String) {
            driver.get("$domain/${NamedPaths.CREATE_PROJECT.path}")
            driver.findElement(By.id(ProjectElements.PROJECT_INPUT.id)).sendKeys(project)
            driver.findElement(By.id(ProjectElements.CREATE_BUTTON.id)).click()
            assertEquals("SUCCESS", driver.title)
        }
    }


}