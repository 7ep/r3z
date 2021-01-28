package coverosR3z.uitests

import org.openqa.selenium.WebDriver

class PageObjectModel {
    lateinit var rp : RegisterPage
    lateinit var lp : LoginPage
    lateinit var llp : LoggingPage
    lateinit var etp : EnterTimePage
    lateinit var eep : EnterEmployeePage
    lateinit var epp : EnterProjectPage
    lateinit var lop : LogoutPage

    companion object {
        fun make(driver: WebDriver, domain: String) : PageObjectModel {
            val pom = PageObjectModel()
            pom.rp = RegisterPage(driver, domain)
            pom.lp = LoginPage(driver, domain)
            pom.etp = EnterTimePage(driver, domain)
            pom.eep = EnterEmployeePage(driver, domain)
            pom.epp = EnterProjectPage(driver, domain)
            pom.llp = LoggingPage(driver, domain)
            pom.lop = LogoutPage(driver, domain)
            return pom
        }
    }
}