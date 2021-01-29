package coverosR3z.uitests

import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import org.openqa.selenium.WebDriver

class PageObjectModel {
    lateinit var rp : RegisterPage
    lateinit var lp : LoginPage
    lateinit var llp : LoggingPage
    lateinit var etp : EnterTimePage
    lateinit var eep : EnterEmployeePage
    lateinit var epp : EnterProjectPage
    lateinit var lop : LogoutPage
    lateinit var businessCode: BusinessCode
    lateinit var server: Server
    lateinit var domain : String
    lateinit var driver: WebDriver
    lateinit var pmd : PureMemoryDatabase

    companion object {
        fun make(driver: WebDriver, port: Int, businessCode: BusinessCode, server: Server, pmd : PureMemoryDatabase) : PageObjectModel {
            val pom = PageObjectModel()

            pom.pmd = pmd
            pom.domain = "http://localhost:$port"
            pom.server = server
            pom.businessCode = businessCode
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