package coverosR3z.uitests

import coverosR3z.logging.LogTypes
import coverosR3z.server.NamedPaths
import coverosR3z.server.Server
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.openqa.selenium.WebDriver
import java.io.File
import kotlin.concurrent.thread



@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UITests {

    @Test
    fun `001 - should get a 404 error on page not found`() {
        driver.get("$domain/does-not-exist")
        assertEquals("404 error", driver.title)
    }

    @Test
    fun `002 - should get a 401 error if I fail to login`() {
        lp.login("userabc", "password12345")
        assertEquals("401 error", driver.title)
    }

    @Test
    fun `003 - should get a 500 error if I post insufficient data to a page`() {
        rp.register("", "password12345", "Administrator")
        assertEquals("500 error", driver.title)
    }

    @Test
    fun `004 - should get a failure message if I register an already-registered user`() {
        rp.register("usera", "password12345", "Administrator")
        rp.register("usera", "password12345", "Administrator")
        assertEquals("FAILURE", driver.title)
    }

    @Test
    fun `005 - should be able to see the homepage and the authenticated homepage`() {
        driver.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Homepage", driver.title)
        rp.register("employeemaker", "password12345", "Administrator")
        lp.login("employeemaker", "password12345")
        driver.get("$domain/${NamedPaths.HOMEPAGE.path}")
        assertEquals("Authenticated Homepage", driver.title)
        logout()
    }

    @Test
    fun `006 - I should be able to change the logging settings`() {
        // Given I am an admin
        rp.register("corey", "password12345", "Administrator")
        lp.login("corey", "password12345")
        llp.go()
        // When I set Warn-level logging to not log
        llp.setLoggingFalse(LogTypes.WARN)
        llp.save()
        llp.go()
        // Then that logging is set to not log
        assertFalse(llp.isLoggingOn(LogTypes.WARN))
        logout()
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    companion object {
        private const val domain = "http://localhost:12345"
        private val webDriver = Drivers.CHROME
        private lateinit var sc : Server
        private const val dbDirectory = "build/db/"
        private lateinit var driver: WebDriver
        private lateinit var rp : RegisterPage
        private lateinit var lp : LoginPage
        private lateinit var llp : LoggingPage
        private lateinit var etp : EnterTimePage
        private lateinit var eep : EnterEmployeePage
        private lateinit var epp : EnterProjectPage
        private lateinit var lop : LogoutPage

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

            rp = RegisterPage(driver, domain)
            lp = LoginPage(driver, domain)
            etp = EnterTimePage(driver, domain)
            eep = EnterEmployeePage(driver, domain)
            epp = EnterProjectPage(driver, domain)
            llp = LoggingPage(driver, domain)
            lop = LogoutPage(driver, domain)
        }

        @AfterClass
        @JvmStatic
        fun shutDown() {
            Server.halfOpenServerSocket.close()
            driver.quit()
        }

    }

    private fun logout() {
        lop.go()
    }

}