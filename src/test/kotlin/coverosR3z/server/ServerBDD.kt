package coverosR3z.server

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomElement
import com.gargoylesoftware.htmlunit.html.HtmlPage
import coverosR3z.main
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
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
        driver.get("localhost:8080/sample_template.utl")

        // Then I see it successfully in the browser
        assertEquals("demo", driver.title)
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
        val page : HtmlPage = driver.getPage("http://localhost:8080/sample_template.utl")
        // Then I see it successfully in the browser
        val result: DomElement = page.getElementById("username")

        assertEquals("Jona", result.textContent)
        driver.close()
    }

}