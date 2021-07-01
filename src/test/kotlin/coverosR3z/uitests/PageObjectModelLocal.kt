package coverosR3z.uitests

import coverosR3z.system.utility.FullSystem
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import org.openqa.selenium.WebDriver

/**
 * Similar to [PageObjectModel] but has access to the
 * server, the [BusinessCode], and the [PureMemoryDatabase]
 */
class PageObjectModelLocal : PageObjectModel() {
    lateinit var businessCode: BusinessCode
    lateinit var fs: FullSystem
    lateinit var pmd : PureMemoryDatabase

    companion object {

        fun make(
            driver: WebDriver,
            port: Int,
            sslPort: Int,
            businessCode: BusinessCode,
            fs: FullSystem,
            pmd : PureMemoryDatabase,
            domain : String = "") : PageObjectModelLocal {

            val pom = PageObjectModelLocal()

            pom.pmd = pmd
            pom.sslDomain = "https://$domain:$sslPort"
            pom.insecureDomain = "http://$domain:$port"
            pom.fs = fs
            pom.businessCode = businessCode
            pom.driver = driver

            pom.rp = RegisterPage(driver, pom.sslDomain)
            pom.lp = LoginPage(driver, pom.sslDomain)
            pom.eep = EnterEmployeePage(driver, pom.sslDomain)
            pom.epp = EnterProjectPage(driver, pom.sslDomain)
            pom.llp = LoggingPage(driver, pom.sslDomain)
            pom.lop = LogoutPage(driver, pom.sslDomain)
            pom.vtp = ViewTimePage(driver, pom.sslDomain)
            pom.ap = AllPages(driver, pom.sslDomain)
            pom.sa = SetApproverPage(driver, pom.sslDomain)
            return pom
        }
    }
}