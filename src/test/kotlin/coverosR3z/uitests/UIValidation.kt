package coverosR3z.uitests

import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.maxPasswordSize
import coverosR3z.authentication.types.maxUserNameSize
import coverosR3z.authentication.types.minPasswordSize
import coverosR3z.authentication.types.minUserNameSize
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class UIValidation {

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
        assertEquals("SUCCESS", driver.title)
    }

    private fun tooShortPassword() {
        pom.rp.register("alice", "a".repeat(minPasswordSize - 1), "Administrator")
        assertEquals("register", driver.title)
    }

    private fun tooLongerUsername() {
        val tooLongUsername = "a".repeat(maxUserNameSize + 1)
        pom.rp.register(tooLongUsername, "password12345", "Administrator")
        assertFalse(pmd.UserDataAccess().read { users -> users.any { it.name.value == tooLongUsername } })
    }

    private fun tooShortUsername() {
        pom.rp.register("a".repeat(minUserNameSize - 1), "password12345", "Administrator")
        assertEquals("register", driver.title)
    }

    private fun disallowBecauseMissingEmployee() {
        driver.get("$domain/${RegisterAPI.path}")
        driver.findElement(By.id("username")).sendKeys("alice")
        driver.findElement(By.id("password")).sendKeys("password12345")
        driver.findElement(By.id("register_button")).click()
        assertEquals("register", driver.title)
    }

    private fun disallowBecauseMissingPassword() {
        pom.rp.register("alice", "", "Administrator")
        assertEquals("register", driver.title)
    }

    private fun disallowBecauseMissingUsername() {
        pom.rp.register("", "password12345", "Administrator")
        assertEquals("register", driver.title)
    }

    companion object {
        private const val port = 4004
        private const val domain = "http://localhost:$port"

        private val webDriver = Drivers.CHROME
        private lateinit var sc : Server
        private lateinit var driver: WebDriver
        private lateinit var businessCode : BusinessCode
        private lateinit var pmd : PureMemoryDatabase
        private lateinit var pom : PageObjectModel


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
        // start the server
        sc = Server(port)
        pmd = Server.makeDatabase()
        businessCode = Server.initializeBusinessCode(pmd)
        sc.startServer(businessCode)

        driver = webDriver.driver()
        pom = PageObjectModel.make(driver, domain)
    }

    @After
    fun cleanup() {
        sc.halfOpenServerSocket.close()
        driver.quit()
    }

}