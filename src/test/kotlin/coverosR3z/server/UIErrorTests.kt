package coverosR3z.server

import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category

class UIErrorTests {

    @Category(UITestCategory::class)
    @Test
    fun `errors - should get a 404 error on page not found`() {
        pom.driver.get("${pom.domain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    @Category(UITestCategory::class)
    @Test
    fun `errors - should get a 401 error if I fail to login`() {
        pom.lp.login("userabc", "password12345")
        assertEquals("401 error", pom.driver.title)
    }

    @Category(UITestCategory::class)
    @Test
    fun `errors - should get a failure message if I register an already-registered user`() {
        pom.rp.register("usera", "password12345", "Administrator")
        pom.rp.register("usera", "password12345", "Administrator")
        assertEquals("FAILURE", pom.driver.title)
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
}