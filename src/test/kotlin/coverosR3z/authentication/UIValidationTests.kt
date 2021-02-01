package coverosR3z.authentication

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.maxPasswordSize
import coverosR3z.authentication.types.maxUserNameSize
import coverosR3z.authentication.types.minPasswordSize
import coverosR3z.authentication.types.minUserNameSize
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITest
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.By

class UIValidationTests {

    @UITest
    @Test
    fun `validation - Validation should stop me entering invalid input on the registration page`() {
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

    companion object {
        private const val port = 4004
        private lateinit var pom : PageObjectModelLocal

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
        pom = startupTestForUI(port = port)
    }

    @After
    fun cleanup() {
        pom.server.halfOpenServerSocket.close()
        pom.driver.quit()
    }

}