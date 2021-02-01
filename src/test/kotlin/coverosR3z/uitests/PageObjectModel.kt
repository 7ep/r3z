package coverosR3z.uitests

import org.openqa.selenium.WebDriver

/**
 * provides an API for testing the UI that is far
 * friendlier and maintainable to the tester
 */
open class PageObjectModel {
    lateinit var rp : RegisterPage
    lateinit var lp : LoginPage
    lateinit var llp : LoggingPage
    lateinit var etp : EnterTimePage
    lateinit var eep : EnterEmployeePage
    lateinit var epp : EnterProjectPage
    lateinit var lop : LogoutPage
    lateinit var domain : String
    lateinit var driver: WebDriver

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
            pom.etp = EnterTimePage(driver, pom.domain)
            pom.eep = EnterEmployeePage(driver, pom.domain)
            pom.epp = EnterProjectPage(driver, pom.domain)
            pom.llp = LoggingPage(driver, pom.domain)
            pom.lop = LogoutPage(driver, pom.domain)
            return pom
        }
    }
}