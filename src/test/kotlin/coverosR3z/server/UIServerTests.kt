package coverosR3z.server

import coverosR3z.logging.LogTypes
import coverosR3z.server.api.HomepageAPI
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

class UIServerTests {

    @UITest
    @Test
    fun `general - should be able to see the homepage and the authenticated homepage`() {
        pom.driver.get("${pom.domain}/${HomepageAPI.path}")
        assertEquals("Homepage", pom.driver.title)
        pom.rp.register("employeemaker", "password12345", "Administrator")
        pom.lp.login("employeemaker", "password12345")
        pom.driver.get("${pom.domain}/${HomepageAPI.path}")
        assertEquals("Authenticated Homepage", pom.driver.title)
        logout()
    }

    @UITest
    @Test
    fun `general - I should be able to change the logging settings`() {
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

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */


    companion object {
        private const val port = 4005
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
        pom.fs.shutdown()
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }

}