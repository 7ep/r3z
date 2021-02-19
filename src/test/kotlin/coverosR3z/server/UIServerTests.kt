package coverosR3z.server

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.maxPasswordSize
import coverosR3z.authentication.types.maxUserNameSize
import coverosR3z.authentication.types.minPasswordSize
import coverosR3z.authentication.types.minUserNameSize
import coverosR3z.logging.LogTypes
import coverosR3z.misc.DEFAULT_DB_DIRECTORY
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.misc.testLogger
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.server.api.HomepageAPI
import coverosR3z.timerecording.UITimeEntryTests
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import java.io.File

class UIServerTests {

    @Category(UITestCategory::class)
    @Test
    fun serverTests() {
        `Go to an unknown page, expecting a not-found error`()
        `Try logging in with invalid credentials, expecting to be forbidden`()
        `Try posting garbage to the registration endpoint, expecting an error`()
        `general - should be able to see the homepage and the authenticated homepage`()
        `general - I should be able to change the logging settings`()
        `validation - Validation should stop me entering invalid input on the registration page`()
    }

    private fun `general - should be able to see the homepage and the authenticated homepage`() {
        pom.driver.get("${pom.domain}/${HomepageAPI.path}")
        assertEquals("Homepage", pom.driver.title)
        pom.rp.register("employeemaker", "password12345", "Administrator")
        pom.lp.login("employeemaker", "password12345")
        pom.driver.get("${pom.domain}/${HomepageAPI.path}")
        assertEquals("Authenticated Homepage", pom.driver.title)
        logout()
    }

    private fun `general - I should be able to change the logging settings`() {
        // Given I am an admin
        pom.rp.register("corey", "password12345", "Administrator")
        pom.lp.login("corey", "password12345")
        pom.llp.go()
        // When I set Warn-level logging to not log
        pom.llp.setLoggingFalse(LogTypes.WARN)
        pom.llp.save()
        pom.llp.go()
        // Then that logging is set to not log
        assertFalse(pom.llp.isLoggingOn(LogTypes.WARN))
        logout()
    }

    private fun `validation - Validation should stop me entering invalid input on the registration page`() {
        // validation won't allow it through - missing username
        disallowBecauseMissingUsername()

        // validation won't allow it through - missing password
        disallowBecauseMissingPassword()

        // validation won't allow it through - missing employee
        disallowBecauseMissingEmployee()

        // validation won't allow it through - username too short
        tooShortUsername()

        // Text entry will stop taking characters at the maximum size, so
        // what gets entered will just be truncated to [maxUserNameSize]
        tooLongerUsername()

        // validation won't allow it through - password too short
        tooShortPassword()

        // Text entry will stop taking characters at the maximum size, so
        // what gets entered will just be truncated to [maxPasswordSize]
        // therefore, if we use a password too long, the system will
        // only record the password that was exactly at max size
        tooLongPassword()
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
        private const val port = 4001
        private lateinit var pom : PageObjectModelLocal
        private lateinit var databaseDirectory : String

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    @Before
    fun init() {
        val databaseDirectorySuffix = "uiservertests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory)
    }

    @After
    fun finish() {
        pom.fs.shutdown()
        val pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }


    private fun `Try posting garbage to the registration endpoint, expecting an error`() {
        pom.rp.register("usera", "password12345", "Administrator")
        pom.rp.register("usera", "password12345", "Administrator")
        assertEquals("FAILURE", pom.driver.title)
    }

    private fun `Try logging in with invalid credentials, expecting to be forbidden`() {
        pom.lp.login("userabc", DEFAULT_PASSWORD.value)
        assertEquals("401 error", pom.driver.title)
    }

    private fun `Go to an unknown page, expecting a not-found error`() {
        pom.driver.get("${pom.domain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    private fun logout() {
        pom.lop.go()
    }

    private fun tooLongPassword() {
        val maxPassword = "a".repeat(maxPasswordSize)
        pom.rp.register("cool", maxPassword + "z", "Administrator")
        pom.lp.login("cool", maxPassword)
        assertEquals("Authenticated Homepage", pom.driver.title)
    }

    private fun tooShortPassword() {
        pom.rp.register("alice", "a".repeat(minPasswordSize - 1), "Administrator")
        assertEquals("register", pom.driver.title)
    }

    private fun tooLongerUsername() {
        val tooLongUsername = "a".repeat(maxUserNameSize + 1)
        pom.rp.register(tooLongUsername, "password12345", "Administrator")
        assertFalse(pom.pmd.UserDataAccess().read { users -> users.any { it.name.value == tooLongUsername } })
    }

    private fun tooShortUsername() {
        pom.rp.register("a".repeat(minUserNameSize - 1), "password12345", "Administrator")
        assertEquals("register", pom.driver.title)
    }

    private fun disallowBecauseMissingEmployee() {
        pom.driver.get("${pom.domain}/${RegisterAPI.path}")
        pom.driver.findElement(By.id("username")).sendKeys("alice")
        pom.driver.findElement(By.id("password")).sendKeys("password12345")
        pom.driver.findElement(By.id("register_button")).click()
        assertEquals("register", pom.driver.title)
    }

    private fun disallowBecauseMissingPassword() {
        pom.rp.register("alice", "", "Administrator")
        assertEquals("register", pom.driver.title)
    }

    private fun disallowBecauseMissingUsername() {
        pom.rp.register("", "password12345", "Administrator")
        assertEquals("register", pom.driver.title)
    }

}