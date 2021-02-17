package coverosR3z.uitests

import coverosR3z.misc.types.Date
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

/**
 * provides an API for testing the UI that is far
 * friendlier and maintainable to the tester
 */
open class PageObjectModel {
    lateinit var rp : RegisterPage
    lateinit var lp : LoginPage
    lateinit var llp : LoggingPage
    lateinit var eep : EnterEmployeePage
    lateinit var epp : EnterProjectPage
    lateinit var lop : LogoutPage
    lateinit var vtp : ViewTimePage
    lateinit var domain : String
    lateinit var driver: WebDriver

    /**
     * Chrome takes date input differently than other browsers.
     * This helps us avoid a bit of boilerplate so we can use
     * the proper
     */
    fun calcDateString(date : Date) : String {
        return if (driver is ChromeDriver) {
            date.chromeStringValue // returns the chrome format
        } else {
            date.stringValue // returns the chrome format
        }
    }

    companion object {
        fun make(
            driver: WebDriver,
            port: Int,
            domain : String = "") : PageObjectModel {
            val pom = PageObjectModel()

            pom.domain = "$domain:$port"
            pom.driver = driver

            pom.rp = RegisterPage(driver, pom.domain)
            pom.lp = LoginPage(driver, pom.domain)
            pom.eep = EnterEmployeePage(driver, pom.domain)
            pom.epp = EnterProjectPage(driver, pom.domain)
            pom.llp = LoggingPage(driver, pom.domain)
            pom.lop = LogoutPage(driver, pom.domain)
            return pom
        }
    }
}