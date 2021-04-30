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
    lateinit var eep : EnterEmployeePage
    lateinit var epp : EnterProjectPage
    lateinit var lop : LogoutPage
    lateinit var vtp : ViewTimePage
    lateinit var insecureDomain: String
    lateinit var sslDomain : String
    lateinit var driver: WebDriver
}