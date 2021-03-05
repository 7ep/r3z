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

class UIRoleTests {

    private val adminUsername = "adrian"
    private val testEmployeeName = "bob bobberton"
    private val testUsername = "plain_ol_bob"
    private val testApproverName = "billy the accountant"
    private val testApproverUsername = "billy_approver12"

    @Category(UITestCategory::class)
    @Test
    fun verifyAdminCanSeeWhatTheyShould() {
        `setup some default projects and employees`()
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

        //create employee
        //login as employee
        //verify the employee only sees what they should see
    }

    @Category(UITestCategory::class)
    @Test
    fun verifyApproverCanSeeWhatTheyShould() {
        `setup some default projects and employees`()
        loginAdmin()
        //create employee
        createApprover()
        logout()
        //login as employee
        registerAndLoginTestApproverUser()

        // validate we are actually the user we intend to be
        val userGreeting = pom.driver.findElement(By.cssSelector("#username")).text
        assertTrue(testApproverUsername in userGreeting)

        //verify the employee only sees what they should see
        val appUrl = pom.sslDomain

        var links = mutableListOf<String>()
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

    @Category(UITestCategory::class)
    @Test
    fun verifyRegularUserCanSeeWhatTheyShould() {

        `setup some default projects and employees`()
        loginAdmin()
        //create employee
        createEmployee()
        logout()
        //login as employee
        registerAndLoginTestUser()

        //verify the employee only sees what they should see
        val appUrl = pom.sslDomain

        var links = mutableListOf<String>()
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
        Assert.assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }

    private fun logout() {
        pom.lop.go()
    }


    private fun loginAdmin() {
        logout()

//        pom.rp.register(adminUsername, DEFAULT_PASSWORD.value, adminEmployee)
        pom.lp.login(adminUsername, DEFAULT_PASSWORD.value)
    }

    private fun createEmployee() {
        pom.eep.enter(testEmployeeName)
    }

    private fun createApprover() {
        pom.eep.enter(testApproverName)
    }

    private fun registerAndLoginTestUser() {
        pom.rp.register(testUsername, "password12345", testEmployeeName)
        pom.lp.login(testUsername, "password12345")
    }

    private fun registerAndLoginTestApproverUser() {
        pom.rp.register(testApproverUsername, "password12345", testApproverName)
        pom.lp.login(testApproverUsername, "password12345")
    }

    private fun `setup some default projects and employees`() {
        logout()
        // register and login the Admin
        pom.rp.register(adminUsername, DEFAULT_PASSWORD.value, DEFAULT_ADMINISTRATOR_NAME.value)
        val user = pom.pmd.dataAccess<User>(User.directoryName).read { users -> users.single{ it.name.value == adminUsername }}
        pom.businessCode.au.addRoleToUser(user, Roles.ADMIN)
        pom.lp.login(adminUsername, DEFAULT_PASSWORD.value)

        // Create a default project
        pom.epp.enter(DEFAULT_PROJECT.toString())
    }
}