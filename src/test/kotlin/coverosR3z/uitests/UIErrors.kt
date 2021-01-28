package coverosR3z.uitests

import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.openqa.selenium.WebDriver

class UIErrors {

    @UITest
    @Test
    fun `errors - should get a 404 error on page not found`() {
        driver.get("${domain}/does-not-exist")
        assertEquals("404 error", driver.title)
    }

    @UITest
    @Test
    fun `errors - should get a 401 error if I fail to login`() {
        pom.lp.login("userabc", "password12345")
        assertEquals("401 error", driver.title)
    }

    @UITest
    @Test
    fun `errors - should get a failure message if I register an already-registered user`() {
        pom.rp.register("usera", "password12345", "Administrator")
        pom.rp.register("usera", "password12345", "Administrator")
        assertEquals("FAILURE", driver.title)
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
        private const val port = 4002
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