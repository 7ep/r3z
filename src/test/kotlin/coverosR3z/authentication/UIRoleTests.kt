package coverosR3z.authentication

import coverosR3z.authentication.types.Roles
import coverosR3z.authentication.types.User
import coverosR3z.misc.*
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.*
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import java.io.File

/**
 * This suite of tests is to ensure that the user's role allows
 * them to see what they ought.  Note: role has nothing to do with employee,
 * only user.  A single employee could potentially have a user that is an
 * admin and a user that is a regular role.  For that reason, we will
 * just use the [DEFAULT_EMPLOYEE] as the employee for all these tests.
 */
class UIRoleTests {

    private val adminUserName = "adrian"
    private val regularUserName = "bob bobberton"
    private val approverUserName = "billy the accountant"

    @Category(UITestCategory::class)
    @Test
    fun uiRoleTests() {
        `setup some projects users and employees`()
        verifyAdminCanSeeWhatTheyShould()
        logout()
        verifyRegularUserCanSeeWhatTheyShould()
        logout()
        verifyApproverCanSeeWhatTheyShould()
    }

    private fun verifyAdminCanSeeWhatTheyShould() {
        loginAdmin()

        val appUrl = pom.sslDomain

        val createEmployeeLink = pom.driver.findElement(By.cssSelector("li:nth-child(1) > a")).getAttribute("href")
        val showAllEmployeesLink = pom.driver.findElement(By.cssSelector("li:nth-child(2) > a")).getAttribute("href")
        val createProjectLink = pom.driver.findElement(By.cssSelector("li:nth-child(3) > a")).getAttribute("href")
        val enterTimeLink = pom.driver.findElement(By.cssSelector("li:nth-child(4) > a")).getAttribute("href")
        val showAllEntriesLink = pom.driver.findElement(By.cssSelector("li:nth-child(5) > a")).getAttribute("href")
        val logConfigLink = pom.driver.findElement(By.cssSelector("li:nth-child(6) > a")).getAttribute("href")
        val logoutLink = pom.driver.findElement(By.cssSelector("li:nth-child(7) > a")).getAttribute("href")

        assertEquals("$appUrl/createemployee", createEmployeeLink)
        assertEquals("$appUrl/employees", showAllEmployeesLink)
        assertEquals("$appUrl/createproject", createProjectLink)
        assertEquals("$appUrl/entertime", enterTimeLink)
        assertEquals("$appUrl/timeentries", showAllEntriesLink)
        assertEquals("$appUrl/logging", logConfigLink)
        assertEquals("$appUrl/logout", logoutLink)
    }

    private fun verifyApproverCanSeeWhatTheyShould() {
        pom.lp.login(approverUserName, DEFAULT_PASSWORD.value)

        // validate we are actually the user we intend to be
        val userGreeting = pom.driver.findElement(By.cssSelector("#username")).text
        assertTrue(approverUserName in userGreeting)

        //verify the employee only sees what they should see
        val appUrl = pom.sslDomain

        val links = mutableListOf<String>()
        // some of these SHOULDN'T be visible. How do we verify we CANT find them?

        for (i in 1..7) {
            try {
                links.add(pom.driver.findElement(By.cssSelector("li:nth-child($i) > a")).getAttribute("href"))
            } catch (e: NoSuchElementException) {}
        }

        // things that should be present
        assertTrue("$appUrl/entertime" in links)
        assertTrue("$appUrl/timeentries" in links)
        assertTrue("$appUrl/logout" in links)

        // things that should not be present
        assertTrue("$appUrl/employees" !in links)
        assertTrue("$appUrl/logging" !in links)
        assertTrue("$appUrl/createemployee" !in links)
        assertTrue("$appUrl/createproject" !in links)
    }

    private fun verifyRegularUserCanSeeWhatTheyShould() {
        pom.lp.login(regularUserName, DEFAULT_PASSWORD.value)

        //verify the employee only sees what they should see
        val appUrl = pom.sslDomain

        val links = mutableListOf<String>()
        // some of these SHOULDN'T be visible. How do we verify we CANT find them?

        for (i in 1..7) {
            try {
                links.add(pom.driver.findElement(By.cssSelector("li:nth-child($i) > a")).getAttribute("href"))
            } catch (e: NoSuchElementException) {}
        }

        // things that should be present
        assertTrue("$appUrl/entertime" in links)
        assertTrue("$appUrl/timeentries" in links)
        assertTrue("$appUrl/logout" in links)

        // things that should not be present
        assertTrue("$appUrl/logging" !in links)
        assertTrue("$appUrl/employees" !in links)
        assertTrue("$appUrl/createemployee" !in links)
        assertTrue("$appUrl/createproject" !in links)
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
        private const val port = 4003
        private lateinit var pom: PageObjectModelLocal
        private lateinit var databaseDirectory : String

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
        val databaseDirectorySuffix = "uiroletests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        pom = startupTestForUI(port = port, directory = databaseDirectory)
    }

    @After
    fun finish() {
        pom.fs.shutdown()
        val pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }

    private fun loginAdmin() {
        pom.lp.login(adminUserName, DEFAULT_PASSWORD.value)
    }

    private fun createApproverUser() {
        pom.rp.register(approverUserName, DEFAULT_PASSWORD.value, DEFAULT_EMPLOYEE_NAME.value)
        val user = pom.pmd.dataAccess<User>(User.directoryName).read { users -> users.single{ it.name.value == approverUserName} }
        pom.businessCode.au.addRoleToUser(user, Roles.APPROVER)
    }

    private fun `setup some projects users and employees`() {
        createAdminUser()
        pom.lp.login(adminUserName, DEFAULT_PASSWORD.value)

        // Create a default project
        pom.epp.enter(DEFAULT_PROJECT.name.value)

        // To make things more explicit, create a default employee that doesn't sound like "administrator", so
        // we don't get confused.
        pom.eep.enter(DEFAULT_EMPLOYEE_NAME.value)

        logout()

        // register our regular user
        // nothing special to do here, everyone starts as a regular user
        pom.rp.register(regularUserName, DEFAULT_PASSWORD.value, DEFAULT_EMPLOYEE_NAME.value)

        // register our approver user
        createApproverUser()
    }

    private fun createAdminUser() {
        pom.rp.register(adminUserName, DEFAULT_PASSWORD.value, DEFAULT_ADMINISTRATOR_NAME.value)
        val user = pom.pmd.dataAccess<User>(User.directoryName)
            .read { users -> users.single { it.name.value == adminUserName } }
        pom.businessCode.au.addRoleToUser(user, Roles.ADMIN)
    }
}