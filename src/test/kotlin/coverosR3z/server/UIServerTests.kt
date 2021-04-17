package coverosR3z.server

import coverosR3z.authentication.api.ChangePasswordAPI
import coverosR3z.authentication.api.LoginAPI
import coverosR3z.authentication.api.RegisterAPI
import coverosR3z.authentication.types.*
import coverosR3z.logging.LogTypes
import coverosR3z.misc.DEFAULT_DATE
import coverosR3z.misc.DEFAULT_DB_DIRECTORY
import coverosR3z.misc.DEFAULT_PASSWORD
import coverosR3z.misc.testLogger
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.server.api.HomepageAPI
import coverosR3z.timerecording.CreateEmployeeUserStory
import coverosR3z.uitests.Drivers
import coverosR3z.uitests.PageObjectModelLocal
import coverosR3z.uitests.UITestCategory
import coverosR3z.uitests.startupTestForUI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.openqa.selenium.By
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import java.io.File

@Category(UITestCategory::class)
class UIServerTests {

    @Test
    fun testWithChrome() {
        serverTests(Drivers.CHROME.driver)
    }

    @Test
    fun testWithFirefox() {
        serverTests(Drivers.FIREFOX.driver)
    }

    /**
     * A UI test for miscellaneous UI behaviors of the system, like
     * going to not-found pages, invalid credentials, dealing with a
     * restart of the system, viewing the homepage, and so on...
     *
     * Also checks that the registration page prevents bad input
     */
    private fun serverTests(driver: () -> WebDriver) {
        init(driver)
        `Go to an unknown page, expecting a not-found error`()
        `Go to an unknown page on an insecure port, expecting a not-found error`()
        `Try logging in with invalid credentials, expecting to be forbidden`()
        `Try hitting the insecure login page, expecting to be redirected to the secure one`()
        `head to the homepage on an insecure endpoint and find ourselves redirected to the SSL endpoint`()
        `general - should be able to see the homepage and the authenticated homepage`()
        `general - I should be able to change the logging settings`()
        `validation - Validation should stop me entering invalid input on the registration page`()
        val (newPassword, hankNewPassword) =
            `change admin password, relogin, create new employee, use invitation and change their password and login`()
        shutdown()
        restart(driver)
        `create a new project`(newPassword)
        `hank enters time`(hankNewPassword)
        shutdown()
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
        private const val port = 4001
        private lateinit var pom : PageObjectModelLocal
        private lateinit var databaseDirectory : String

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()
        }

    }

    fun init(driver: () -> WebDriver) {
        val databaseDirectorySuffix = "uiservertests_on_port_$port"
        databaseDirectory = "$DEFAULT_DB_DIRECTORY$databaseDirectorySuffix/"
        File(databaseDirectory).deleteRecursively()
        createPom(driver)
    }

    private fun createPom(driver: () -> WebDriver) {
        pom = startupTestForUI(port = port, directory = databaseDirectory, driver = driver)

        // Each UI test puts the window in a different place around the screen
        // so we have a chance to see what all is going on
        pom.driver.manage().window().position = Point(400, 200)
    }

    private fun restart(driver: () -> WebDriver) {
        createPom(driver)
    }

    private fun shutdown() {
        pom.fs.shutdown()
        val pmd = DatabaseDiskPersistence(databaseDirectory, testLogger).startWithDiskPersistence()
        assertEquals(pom.pmd, pmd)
        pom.driver.quit()
    }

    private fun `hank enters time`(hankNewPassword: String) {
        pom.lp.login("hank", hankNewPassword)
        pom.vtp.enterTime("great new project", "2", "these are some details", DEFAULT_DATE)
    }

    private fun `create a new project`(newPassword: String) {
        pom.lp.login("employeemaker", newPassword)
        pom.epp.enter("great new project")
        logout()
    }

    private fun `change admin password, relogin, create new employee, use invitation and change their password and login`(): Pair<String, String> {
        pom.lp.login("employeemaker", "password12345")
        // go to the change password page
        pom.driver.get("${pom.sslDomain}/${ChangePasswordAPI.path}")
        // confirm the title
        assertEquals("change password", pom.driver.title)
        // disagree to change the password
        pom.driver.findElement(By.id(ChangePasswordAPI.Elements.BRING_ME_BACK.getId())).click()
        // confirm we are back on the homepage
        assertEquals("Authenticated Homepage", pom.driver.title)
        // go back to the change password page
        pom.driver.get("${pom.sslDomain}/${ChangePasswordAPI.path}")
        // agree this time to change the password
        pom.driver.findElement(By.id(ChangePasswordAPI.Elements.YES_BUTTON.getId())).click()
        // confirm the title
        assertEquals("New password generated", pom.driver.title)
        val newAdminPassword = pom.driver.findElement(By.id(ChangePasswordAPI.Elements.NEW_PASSWORD.getId())).text
        logout()
        // confirm the new password works
        pom.lp.login("employeemaker", newAdminPassword)
        assertEquals("Authenticated Homepage", pom.driver.title)
        // create a new employee
        val s = CreateEmployeeUserStory.getScenario("createEmployee - an invitation is created")
        s.markDone("Given the company has hired a new employee, Hank,")
        s.markDone("when I add him as an employee,")
        pom.eep.enter("hank")
        val invitationUrl = pom.driver.findElement(By.xpath("//*[text() = 'hank']/..//a")).getAttribute("href")
        s.markDone("when I add him as an employee,")
        logout()
        val i = CreateEmployeeUserStory.getScenario("createEmployee - using an invitation to register a user")
        i.markDone("Given an invitation has been created for a new employee, Hank")
        pom.driver.get(invitationUrl)
        i.markDone("When he uses that invitation")
        pom.driver.findElement(By.id("username")).sendKeys("hank")
        pom.driver.findElement(By.id("register_button")).click()
        i.markDone("Then he is able to register")
        val hankRegistrationPassword = pom.driver.findElement(By.id(RegisterAPI.Elements.NEW_PASSWORD.getId())).text
        pom.lp.login("hank", hankRegistrationPassword)
        // go to the change password page
        pom.driver.get("${pom.sslDomain}/${ChangePasswordAPI.path}")
        // agree to change the password
        pom.driver.findElement(By.id(ChangePasswordAPI.Elements.YES_BUTTON.getId())).click()
        // confirm the title
        assertEquals("New password generated", pom.driver.title)
        val hankNewPassword = pom.driver.findElement(By.id(ChangePasswordAPI.Elements.NEW_PASSWORD.getId())).text
        logout()
        // confirm the new password works
        pom.lp.login("hank", hankNewPassword)
        assertEquals("Your time entries", pom.driver.title)
        logout()
        return Pair(newAdminPassword, hankNewPassword)
    }

    private fun `head to the homepage on an insecure endpoint and find ourselves redirected to the SSL endpoint`() {
        pom.driver.get("${pom.insecureDomain}/${HomepageAPI.path}")
        assertEquals("login page", pom.driver.title)
    }

    private fun `general - should be able to see the homepage and the authenticated homepage`() {
        pom.driver.get("${pom.sslDomain}/${HomepageAPI.path}")
        assertEquals("login page", pom.driver.title)
        val adminEmployee = pom.businessCode.tru.listAllEmployees().single{it.name.value == "Administrator"}
        val (_, user) = pom.businessCode.au.registerWithEmployee(UserName("employeemaker"), Password("password12345"), adminEmployee)
        pom.businessCode.au.addRoleToUser(user, Role.ADMIN)
        pom.lp.login("employeemaker", "password12345")
        pom.driver.get("${pom.sslDomain}/${HomepageAPI.path}")
        assertEquals("Authenticated Homepage", pom.driver.title)
        logout()
    }

    private fun `general - I should be able to change the logging settings`() {
        // Given I am an admin
        pom.lp.login("employeemaker", "password12345")
        pom.llp.go()
        // When I set Warn-level logging to not log
        pom.llp.setLoggingFalse(LogTypes.WARN)
        pom.llp.save()
        pom.llp.go()
        // Then that logging is set to not log
        assertFalse(pom.llp.isLoggingOn(LogTypes.WARN))
        logout()
    }

    private fun `validation - Validation should stop me entering invalid input on the registration page`() {
        pom.lp.login("employeemaker", "password12345")
        pom.eep.enter("alice")
        val invitationCode = pom.businessCode.au.listAllInvitations().single {it.employee.name.value == "alice"}

        logout()

        // validation won't allow it through - missing username
        disallowBecauseMissingUsername(invitationCode)

        // validation won't allow it through - username too short
        tooShortUsername(invitationCode)

        // Text entry will stop taking characters at the maximum size, so
        // what gets entered will just be truncated to [maxUserNameSize]
        tooLongerUsername(invitationCode)
    }

    private fun `Try logging in with invalid credentials, expecting to be forbidden`() {
        pom.lp.login("userabc", DEFAULT_PASSWORD.value)
        assertEquals("401 error", pom.driver.title)
    }

    private fun `Try hitting the insecure login page, expecting to be redirected to the secure one`() {
        pom.driver.get("${pom.insecureDomain}/${LoginAPI.path}")
        assertEquals("login page", pom.driver.title)
    }

    private fun `Go to an unknown page, expecting a not-found error`() {
        pom.driver.get("${pom.sslDomain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    private fun `Go to an unknown page on an insecure port, expecting a not-found error`() {
        pom.driver.get("${pom.insecureDomain}/does-not-exist")
        assertEquals("404 error", pom.driver.title)
    }

    private fun logout() {
        pom.lop.go()
    }

    private fun tooLongerUsername(invitation: Invitation) {
        val tooLongUsername = "a".repeat(maxUserNameSize + 1)
        pom.rp.register(tooLongUsername, invitation.code.value)
        assertFalse(pom.pmd.dataAccess<User>(User.directoryName).read { users -> users.any { it.name.value == tooLongUsername } })
    }

    private fun tooShortUsername(invitation: Invitation) {
        pom.rp.register("a".repeat(minUserNameSize - 1), invitation.code.value)
        assertEquals("register", pom.driver.title)
    }

    private fun disallowBecauseMissingUsername(invitation: Invitation) {
        pom.rp.register("", invitation.code.value)
        assertEquals("register", pom.driver.title)
    }

}